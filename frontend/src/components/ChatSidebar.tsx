import React from 'react';
import { ChatSession } from '../types/Message';
import './ChatSidebar.css';

interface ChatSidebarProps {
  chats: ChatSession[];
  currentChatId: string | null;
  onNewChat: () => void;
  onSelectChat: (chatId: string) => void;
  onDeleteChat: (chatId: string) => void;
}

export const ChatSidebar: React.FC<ChatSidebarProps> = ({
  chats,
  currentChatId,
  onNewChat,
  onSelectChat,
  onDeleteChat
}) => {
  const formatDate = (date: Date) => {
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));
    
    if (days === 0) return 'Dzisiaj';
    if (days === 1) return 'Wczoraj';
    if (days < 7) return `${days} dni temu`;
    return date.toLocaleDateString('pl-PL', { day: 'numeric', month: 'short' });
  };

  return (
    <div className="chat-sidebar">
      <div className="sidebar-header">
        <button className="new-chat-button" onClick={onNewChat}>
          <span>➕</span>
          <span>Nowa rozmowa</span>
        </button>
      </div>
      
      <div className="chat-list">
        {chats.map((chat) => (
          <div
            key={chat.id}
            className={`chat-item ${currentChatId === chat.id ? 'active' : ''}`}
            onClick={() => onSelectChat(chat.id)}
          >
            <div className="chat-item-content">
              <div className="chat-item-title">{chat.title}</div>
              <div className="chat-item-date">{formatDate(chat.updatedAt)}</div>
            </div>
            <button
              className="delete-chat-button"
              onClick={(e) => {
                e.stopPropagation();
                onDeleteChat(chat.id);
              }}
              title="Usuń rozmowę"
            >
              🗑️
            </button>
          </div>
        ))}
      </div>
    </div>
  );
};
