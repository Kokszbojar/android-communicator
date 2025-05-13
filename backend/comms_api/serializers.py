from rest_framework import serializers
from comms_api.models import Message, Call


class MessageSerializer(serializers.ModelSerializer):
    class Meta:
        model = Message
        fields = '__all__'


class CallSerializer(serializers.ModelSerializer):
    class Meta:
        model = Call
        fields = '__all__'
