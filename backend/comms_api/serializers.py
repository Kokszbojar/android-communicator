from rest_framework import serializers
from django.contrib.auth.models import User
from comms_api.models import Message, Call, FriendRequest


class MessageSerializer(serializers.ModelSerializer):
    sender_name = serializers.CharField(source="sender.username")

    class Meta:
        model = Message
        fields = ["id", "sender_name", "content", "timestamp"]


class CallSerializer(serializers.ModelSerializer):
    class Meta:
        model = Call
        fields = '__all__'


class FriendRequestSerializer(serializers.ModelSerializer):
    from_user = serializers.ReadOnlyField(source='from_user.id')
    to_user = serializers.PrimaryKeyRelatedField(queryset=User.objects.all())

    class Meta:
        model = FriendRequest
        fields = ['id', 'from_user', 'to_user', 'status', 'created_at', 'responded_at']
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
            from_user=self.context['request'].user,
            **validated_data
        )


class UserSerializer(serializers.ModelSerializer):
    class Meta:
        model = User
        fields = ["id", "username"]
