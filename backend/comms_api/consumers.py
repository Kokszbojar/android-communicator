from channels.generic.websocket import AsyncWebsocketConsumer
from asgiref.sync import sync_to_async
import json
from django.contrib.auth.models import AnonymousUser, User
from comms_api.models import Message, Call


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
        elif action == "call_user":
            await self.handle_call_user(data)
        elif action == "answer_call":
            await self.handle_answer_call(data)

    async def handle_send_message(self, data):
        from_user = self.user
        to_user_id = data["to"]
        content = data["message"]
        to_user = await sync_to_async(User.objects.get)(id=to_user_id)

        message = await sync_to_async(Message.objects.create)(
            sender=from_user,
            recipient=to_user,
            content=content
        )

        await self.channel_layer.group_send(
            f"user_{to_user_id}",
            {
                "type": "chat.message",
                "message": {
                    "from": from_user.id,
                    "content": content,
                    "timestamp": str(message.timestamp),
                },
            }
        )

    async def handle_call_user(self, data):
        callee_id = data["to"]
        callee = await sync_to_async(User.objects.get)(id=callee_id)

        call = await sync_to_async(Call.objects.create)(
            caller=self.user,
            callee=callee,
        )

        await self.channel_layer.group_send(
            f"user_{callee_id}",
            {
                "type": "call.incoming",
                "call": {
                    "caller": self.user.id,
                    "call_id": call.id,
                },
            }
        )

    async def handle_answer_call(self, data):
        call_id = data["call_id"]
        accepted = data["accepted"]

        call = await sync_to_async(Call.objects.get)(id=call_id)
        call.accepted = accepted
        await sync_to_async(call.save)()

        await self.channel_layer.group_send(
            f"user_{call.caller.id}",
            {
                "type": "call.answer",
                "call": {
                    "accepted": accepted,
                    "callee": self.user.id,
                },
            }
        )

    async def chat_message(self, event):
        await self.send(text_data=json.dumps({
            "type": "chat_message",
            **event["message"],
        }))

    async def call_incoming(self, event):
        await self.send(text_data=json.dumps({
            "type": "incoming_call",
            **event["call"],
        }))

    async def call_answer(self, event):
        await self.send(text_data=json.dumps({
            "type": "call_answer",
            **event["call"],
        }))
