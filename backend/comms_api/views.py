from livekit.api import AccessToken, VideoGrants
from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework_simplejwt.tokens import RefreshToken, TokenError
from rest_framework.decorators import api_view, permission_classes
from django.contrib.auth.models import User
from django.contrib.auth import authenticate
from django.db.models import Q
from comms_api.models import Message
from comms_api.serializers import MessageSerializer


class RegisterView(APIView):
    def post(self, request):
        username = request.data.get("username")
        password = request.data.get("password")
        if User.objects.filter(username=username).exists():
            return Response({"error": "Użytkownik już istnieje"}, status=status.HTTP_400_BAD_REQUEST)
        user = User.objects.create_user(username=username, password=password)
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
            (Q(sender=user) & Q(recipient_id=other_user_id)) |
            (Q(sender_id=other_user_id) & Q(recipient=user))
        ).order_by("-timestamp")[offset: offset + limit]

        serialized = MessageSerializer(messages, many=True)
        return Response(serialized.data)


@api_view(['GET'])
@permission_classes([IsAuthenticated])
def livekit_token_view(request):
    user = request.user
    room_name = request.query_params.get('room', '*')  # np. room=chat_123

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
            #can_publish_sources=["camera", "microphone"]
        )).to_jwt()
    print(token)
    return Response({"token": token})

