from urllib.parse import parse_qs
from channels.middleware import BaseMiddleware
from channels.db import database_sync_to_async
from django.contrib.auth.models import AnonymousUser
from rest_framework_simplejwt.tokens import UntypedToken
from rest_framework_simplejwt.exceptions import InvalidToken
from django.contrib.auth.models import User
from rest_framework_simplejwt.authentication import JWTAuthentication


@database_sync_to_async
def get_user(validated_token):
    try:
        jwt_auth = JWTAuthentication()
        return jwt_auth.get_user(validated_token)
    except:
        return AnonymousUser()


class JWTAuthMiddleware(BaseMiddleware):
    async def __call__(self, scope, receive, send):
        query_string = scope.get("query_string", b"").decode()
        query_params = parse_qs(query_string)
        token = query_params.get("token", [None])[0]

        if token:
            try:
                validated_token = JWTAuthentication().get_validated_token(token)
                print(validated_token)
                scope["user"] = await get_user(validated_token)
            except InvalidToken:
                scope["user"] = AnonymousUser()
        else:
            scope["user"] = AnonymousUser()

        return await super().__call__(scope, receive, send)
