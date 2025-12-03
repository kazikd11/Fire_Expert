import React from 'react';
import { Message } from '../types/Message';
import './ChatMessage.css';

interface ChatMessageProps {
  message: Message;
  isStreaming?: boolean;
}

export const ChatMessage: React.FC<ChatMessageProps> = ({ message, isStreaming }) => {
  const isUser = message.role === 'user';

  return (
    <div className={`message-wrapper ${isUser ? 'user-message' : 'assistant-message'}`}>
      <div className="message-container">
        <div className="message-avatar">
          {isUser ? (
            <div className="avatar user-avatar">👤</div>
          ) : (
            <div className="avatar assistant-avatar">🔥</div>
          )}
        </div>
        <div className="message-content">
          <div className="message-role">
            {isUser ? 'Ty' : 'Fire Expert AI'}
          </div>
          <div className="message-text">
            {message.content}
            {isStreaming && <span className="cursor">▊</span>}
          </div>
        </div>
      </div>
    </div>
  );
};
