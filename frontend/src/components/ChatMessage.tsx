import React from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Message } from '../types/Message';
import './ChatMessage.css';

interface ChatMessageProps {
  message: Message;
}

export const ChatMessage: React.FC<ChatMessageProps> = ({ message }) => {
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
            {isUser ? 'Ty' : 'BHP Expert'}
          </div>
          <div className="message-text">
            {isUser ? (
              message.content
            ) : (
              <ReactMarkdown remarkPlugins={[remarkGfm]}>
                {message.content}
              </ReactMarkdown>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
