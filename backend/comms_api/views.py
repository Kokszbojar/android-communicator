from livekit.api import AccessToken, VideoGrants
from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import generics, status
from rest_framework.permissions import IsAuthenticated
from rest_framework_simplejwt.tokens import RefreshToken, TokenError
from rest_framework.decorators import api_view, permission_classes
from django.contrib.auth.models import User
from django.contrib.auth import authenticate
from django.db.models import Q
from comms_api.mqtt_client import send_notification
from comms_api.models import Message, FriendRequest, UserFCMToken
from comms_api.serializers import MessageSerializer, FriendRequestSerializer, UserSerializer


class RegisterView(APIView):
    def post(self, request):
        username = request.data.get("username")
        password = request.data.get("password")
        if User.objects.filter(username=username).exists():
            return Response({"error": "Użytkownik już istnieje"}, status=status.HTTP_400_BAD_REQUEST)
        User.objects.create_user(username=username, password=password)
        return Response({"message": "Zarejestrowano pomyślnie"}, status=status.HTTP_201_CREATED)


class LoginView(APIView):
    def post(self, request):
        username = request.data.get("username")
        password = request.data.get("password")
        user = authenticate(username=username, password=password)
        if user is not None:
            refresh = RefreshToken.for_user(user)
            return Response({
                "refresh": str(refresh),
                "access": str(refresh.access_token),
                "userId": user.id
            })
        return Response({"error": "Nieprawidłowe dane logowania"}, status=status.HTTP_401_UNAUTHORIZED)


class RefreshTokenView(APIView):
    def post(self, request):
        refresh = request.data.get("refresh")
        if not refresh:
            return Response({"error": "Brak refresh tokena"}, status=status.HTTP_400_BAD_REQUEST)

        try:
            token = RefreshToken(refresh)
            return Response({
                "refresh": str(token),
                "access": str(token.access_token)
            })
        except TokenError as e:
            return Response({"error": "Token nieważny lub wygasł", "details": str(e)}, status=status.HTTP_401_UNAUTHORIZED)


class ChatHistoryView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        user = request.user
        other_user_id = request.query_params.get("user_id")
        limit = int(request.query_params.get("limit", 50))
        offset = int(request.query_params.get("offset", 0))

        if not other_user_id:
            return Response({"error": "user_id is required"}, status=400)

        messages = Message.objects.filter(
            Q(sender=user, recipient__id=other_user_id) |
            Q(sender__id=other_user_id, recipient=user)
        ).order_by("-timestamp")[offset: offset + limit]
        
        serialized = MessageSerializer(messages, many=True, context={"request": request})
        return Response({"data": serialized.data, "friendName": User.objects.get(id=other_user_id).username})


@api_view(['GET'])
@permission_classes([IsAuthenticated])
def livekit_token_view(request):
    user = request.user
    room_name = request.query_params.get('room', '*')

    api_key = "devkey"
    api_secret = "secret"

    token = AccessToken(api_key, api_secret) \
        .with_identity(f"{user.id}") \
        .with_name(user.username) \
        .with_grants(VideoGrants(
            room_join=True,
            room=room_name,
            can_publish=True,
            can_publish_data=True,
            can_subscribe=True,
            # can_publish_sources=["camera", "microphone"]
        )).to_jwt()

    if user.id != int(room_name):
        send_notification(
            to_user_id=room_name,
            payload={
                "type": "incoming_call",
                "title": f"{user.username} dzwoni do Ciebie",
                "body": "Kliknij aby odpowiedzieć na połączenie",
                "room_id": room_name
            }
        )
    return Response({"token": token})


class SendFriendRequestView(generics.CreateAPIView):
    permission_classes = [IsAuthenticated]
    serializer_class = FriendRequestSerializer

    def perform_create(self, serializer):
        serializer.save(from_user=self.request.user)


class RespondToFriendRequestView(APIView):
    permission_classes = [IsAuthenticated]

    def patch(self, request, pk):
        try:
            fr = FriendRequest.objects.get(pk=pk)
        except FriendRequest.DoesNotExist:
            return Response({"detail": "Nie znaleziono zaproszenia."}, status=status.HTTP_404_NOT_FOUND)

        fr.status = FriendRequest.Status.ACCEPTED
        fr.save()
        return Response({"detail": "Zaproszenie zaakceptowane."})

    def delete(self, request, pk):
        try:
            fr = FriendRequest.objects.get(pk=pk)
        except FriendRequest.DoesNotExist:
            return Response({"detail": "Nie znaleziono zaproszenia."}, status=status.HTTP_404_NOT_FOUND)

        deleted_count, _ = fr.delete()

        if deleted_count:
            return Response({"message": "Zaproszenie usunięte."}, status=status.HTTP_204_NO_CONTENT)
        else:
            return Response({"error": "Zaproszenie do usunięcia nie istnieje."}, status=status.HTTP_404_NOT_FOUND)


class FriendRequestsView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        received = FriendRequest.objects.filter(to_user=request.user, status=FriendRequest.Status.PENDING)
        sent = FriendRequest.objects.filter(from_user=request.user, status=FriendRequest.Status.PENDING)
        return Response({
            "received": FriendRequestSerializer(received, many=True).data,
            "sent": FriendRequestSerializer(sent, many=True).data
        })


class FriendsListView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        user = request.user
        friendships = FriendRequest.objects.filter(
            Q(from_user=user) | Q(to_user=user),
            status=FriendRequest.Status.ACCEPTED
        )

        friend_ids = set()
        for fr in friendships:
            if fr.from_user == user:
                friend_ids.add(fr.to_user.id)
            else:
                friend_ids.add(fr.from_user.id)

        friends = User.objects.filter(id__in=friend_ids)
        return Response(UserSerializer(friends, many=True, context={"request": request}).data)


class RemoveFriendView(APIView):
    permission_classes = [IsAuthenticated]

    def delete(self, request, user_id):
        user = request.user
        try:
            friend = User.objects.get(id=user_id)
        except User.DoesNotExist:
            return Response({"error": "User not found."}, status=status.HTTP_404_NOT_FOUND)

        deleted_count, _ = FriendRequest.objects.filter(
            status=FriendRequest.Status.ACCEPTED
        ).filter(
            (Q(from_user=user) & Q(to_user=friend)) |
            (Q(from_user=friend) & Q(to_user=user))
        ).delete()

        if deleted_count:
            return Response({"message": "Friend removed."}, status=status.HTTP_204_NO_CONTENT)
        else:
            return Response({"error": "Friendship not found."}, status=status.HTTP_404_NOT_FOUND)


class UserSearchView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        query = request.GET.get("q", "")
        if not query:
            return Response([])

        users = User.objects.filter(username__icontains=query).exclude(id=request.user.id)
        results = []

        for user in users:
            request_sent = FriendRequest.objects.filter(from_user=request.user, to_user=user, status=FriendRequest.Status.PENDING).exists()
            request_received = FriendRequest.objects.filter(from_user=user, to_user=request.user, status=FriendRequest.Status.PENDING).exists()

            results.append({
                "id": user.id,
                "username": user.username,
                "requestSent": request_sent,
                "requestReceived": request_received
            })

        return Response(results)


class UpdateFCMTokenView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request):
        token = request.data.get("token")
        user = request.user
        if token:
            UserFCMToken.objects.update_or_create(user=user, defaults={"token": token})
        return Response({"status": "updated"})
