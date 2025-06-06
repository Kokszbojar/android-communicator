from rest_framework import serializers
from django.contrib.auth.models import User
from django.db.models import Q
from django.utils import timezone
from comms_api.models import Message, Call, FriendRequest


class MessageSerializer(serializers.ModelSerializer):
    sender_name = serializers.CharField(source="sender.username")
    file_url = serializers.SerializerMethodField()

    class Meta:
        model = Message
        fields = ["id", "sender_name", "content", "file_url", "file_type", "timestamp"]

    def get_file_url(self, obj):
        request = self.context.get("request")
        if obj.file and hasattr(obj.file, "url"):
            if request:
                return request.build_absolute_uri(obj.file.url)
            return obj.file.url
        return None


class CallSerializer(serializers.ModelSerializer):
    class Meta:
        model = Call
        fields = '__all__'


class FriendRequestSerializer(serializers.ModelSerializer):
    from_user = serializers.ReadOnlyField(source='from_user.username')
    to_user = serializers.PrimaryKeyRelatedField(queryset=User.objects.all())
    friend = serializers.ReadOnlyField(source='to_user.username')

    class Meta:
        model = FriendRequest
        fields = ['id', 'from_user', 'to_user', 'friend', 'status', 'created_at', 'responded_at']
        read_only_fields = ['status', 'created_at', 'responded_at']

    def validate(self, data):
        from_user = self.context['request'].user
        to_user = data['to_user']

        if from_user == to_user:
            raise serializers.ValidationError("Nie możesz zaprosić samego siebie.")

        if FriendRequest.objects.filter(
            from_user=from_user, to_user=to_user, status=FriendRequest.Status.PENDING
        ).exists():
            raise serializers.ValidationError("Zaproszenie już zostało wysłane.")

        if FriendRequest.objects.filter(
            from_user=to_user, to_user=from_user, status=FriendRequest.Status.PENDING
        ).exists():
            raise serializers.ValidationError("Masz oczekujące zaproszenie od tej osoby.")

        return data

    def create(self, validated_data):
        return FriendRequest.objects.create(
            **validated_data
        )


class UserSerializer(serializers.ModelSerializer):
    lastMessage = serializers.SerializerMethodField()
    hasNewMessage = serializers.SerializerMethodField()
    timestamp = serializers.SerializerMethodField()

    class Meta:
        model = User
        fields = ["id", "username", "lastMessage", "hasNewMessage", "timestamp"]

    def get_lastMessage(self, obj):
        user = self.context.get("request").user
        message = Message.objects.filter(
            Q(sender=user, recipient=obj) | Q(sender=obj, recipient=user)
        ).order_by("-timestamp").first()

        return message.content if message else None
    
    def get_hasNewMessage(self, obj):
        user = self.context.get("request").user
        message = Message.objects.filter(
            Q(sender=user, recipient=obj) | Q(sender=obj, recipient=user)
        ).order_by("-timestamp").first()

        return not message.is_read

    def get_timestamp(self, obj):
        user = self.context.get("request").user
        message = Message.objects.filter(
            Q(sender=user, recipient=obj) | Q(sender=obj, recipient=user)
        ).order_by("-timestamp").first()

        if not message:
            return None

        now = timezone.now()
        delta = now - message.timestamp

        seconds = delta.total_seconds()
        minutes = seconds // 60
        hours = minutes // 60
        days = delta.days

        if minutes < 1:
            return "just now"
        elif minutes < 60:
            return f"{int(minutes)} m"
        elif hours < 24:
            return f"{int(hours)} h"
        elif days < 30:
            return f"{int(days)} d"
        else:
            return message.timestamp.strftime("%Y-%m-%d")  # fallback: exact date
