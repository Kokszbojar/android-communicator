"""
URL configuration for django_project project.

The `urlpatterns` list routes URLs to views. For more information please see:
    https://docs.djangoproject.com/en/5.2/topics/http/urls/
Examples:
Function views
    1. Add an import:  from my_app import views
    2. Add a URL to urlpatterns:  path('', views.home, name='home')
Class-based views
    1. Add an import:  from other_app.views import Home
    2. Add a URL to urlpatterns:  path('', Home.as_view(), name='home')
Including another URLconf
    1. Import the include() function: from django.urls import include, path
    2. Add a URL to urlpatterns:  path('blog/', include('blog.urls'))
"""
from django.contrib import admin
from django.conf import settings
from django.conf.urls.static import static
from django.urls import path
from comms_api.views import (
    RegisterView, LoginView, RefreshTokenView, ChatHistoryView,
    livekit_token_view, SendFriendRequestView, RespondToFriendRequestView,
    FriendRequestsView, FriendsListView, RemoveFriendView, UserSearchView,
    UpdateFCMTokenView
)

urlpatterns = [
    path('admin/', admin.site.urls),

    path('api/register/', RegisterView.as_view()),
    path('api/login/', LoginView.as_view()),

    path("api/token/refresh/", RefreshTokenView.as_view()),
    path('api/livekit-token/', livekit_token_view),

    path("api/chat/history/", ChatHistoryView.as_view()),

    path("api/users/search/", UserSearchView.as_view(), name="user-search"),

    path("api/friends/", FriendsListView.as_view(), name="friends_list"),
    path("api/friends/request/", SendFriendRequestView.as_view(), name="send_friend_request"),
    path("api/friends/requests/", FriendRequestsView.as_view(), name="list_friend_requests"),
    path("api/friends/request/<int:pk>/", RespondToFriendRequestView.as_view(), name="respond_friend_request"),
    path("api/friends/remove/<int:user_id>/", RemoveFriendView.as_view(), name="remove_friend"),

    path("api/fcm/update/", UpdateFCMTokenView.as_view(), name="update-fcm"),
]
if settings.DEBUG:
    urlpatterns += static(settings.MEDIA_URL,
                          document_root=settings.MEDIA_ROOT)
