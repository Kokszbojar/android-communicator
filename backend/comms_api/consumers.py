import json
import base64
from comms_api.mqtt_client import send_notification
from channels.generic.websocket import AsyncWebsocketConsumer
from asgiref.sync import sync_to_async
from django.core.files.base import ContentFile
from django.contrib.auth.models import AnonymousUser, User
from comms_api.models import Message, Call, UserFCMToken
from comms_api.serializers import MessageSerializer


class ChatConsumer(AsyncWebsocketConsumer):
    async def connect(self):
        user = self.scope["user"]
        if user == AnonymousUser():
            await self.close()
        else:
            self.user = user
            self.room_name = f"user_{user.id}"
            await self.channel_layer.group_add(self.room_name, self.channel_name)
            await self.accept()

    async def disconnect(self, close_code):
        if hasattr(self, "room_name"):
            await self.channel_layer.group_discard(self.room_name, self.channel_name)
        else:
            await self.close()

    async def receive(self, text_data):
        data = json.loads(text_data)
        action = data.get("action")

        if action == "send_message":
            await self.handle_send_message(data)

    async def handle_send_message(self, data):
        from_user = self.user
        to_user_id = data["to"]
        content = data["message"]
        to_user = await sync_to_async(User.objects.get)(id=to_user_id)
        file = None
        file_type = ""
        if "file" in data.keys():
            file_data = data["file"]
            file_type = data["file_type"]
            format, file_str = file_data.split(";base64,")
            ext = format.split("/")[-1]
            decoded_file = ContentFile(base64.b64decode(file_str), name=f"message_{from_user.id}_{to_user_id}.{ext}")
            file = decoded_file

        message = await sync_to_async(Message.objects.create)(
            sender=from_user,
            recipient=to_user,
            content=content,
            file=file,
            file_type=file_type or ""
        )

        await self.channel_layer.group_send(
            f"user_{to_user_id}",
            {
                "type": "chat.message",
                "message": {
                    "sender_name": from_user.username,
                    "content": content,
                    "file_url": f"http://192.168.0.130:8000{message.file.url}" if message.file else None,
                    "file_type": file_type,
                    "timestamp": "Teraz",
                },
            }
        )

        send_notification(
            to_user_id=to_user.id,
            payload={
                "type": "chat_message",
                "title": from_user.username,
                "body": content[:60],
                "chat_id": from_user.id
            }
        )

    async def chat_message(self, event):
        await self.send(text_data=json.dumps({
            "type": "chat_message",
            **event["message"],
        }))
