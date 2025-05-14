from rest_framework import serializers
from comms_api.models import Message, Call


class MessageSerializer(serializers.ModelSerializer):
    sender_name = serializers.CharField(source="sender.username")

    class Meta:
        model = Message
        fields = ["id", "sender_name", "content", "timestamp"]


class CallSerializer(serializers.ModelSerializer):
    class Meta:
        model = Call
        fields = '__all__'
