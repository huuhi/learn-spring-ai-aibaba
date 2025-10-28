// 聊天功能JavaScript代码
document.addEventListener('DOMContentLoaded', function () {
    const chatMessages = document.getElementById('chatMessages');
    const messageInput = document.getElementById('messageInput');
    const sendButton = document.getElementById('sendButton');
    const knowledgeBaseSelect = document.getElementById('knowledgeBase');
    const modelSelect = document.getElementById('model');
    const webSearchCheckbox = document.getElementById('webSearch');
    const thinkingCheckbox = document.getElementById('thinking');
    const autoRagCheckbox = document.getElementById('autoRag');
    const newConversationBtn = document.getElementById('newConversationBtn');
    
    // 会话ID存储
    let conversationId = null;
    
    // 缓冲区用于处理不完整的数据块
    let buffer = '';

    // 发送消息
    sendButton.addEventListener('click', sendMessage);
    messageInput.addEventListener('keydown', function (e) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
    });
    
    // 新建对话
    newConversationBtn.addEventListener('click', newConversation);

    // 新建对话函数
    function newConversation() {
        // 清空聊天记录
        chatMessages.innerHTML = '';
        
        // 添加AI欢迎消息
        const welcomeMessage = document.createElement('div');
        welcomeMessage.className = 'message ai-message';
        welcomeMessage.innerHTML = `
            <div class="message-content">
                您好！我是AI助手，请问有什么可以帮助您的吗？
            </div>
        `;
        chatMessages.appendChild(welcomeMessage);
        
        // 清空会话ID
        conversationId = null;
        
        // 清空缓冲区
        buffer = '';
        
        // 清空输入框
        messageInput.value = '';
    }

    // 发送消息函数
    function sendMessage() {
        const message = messageInput.value.trim();
        if (!message) return;

        // 添加用户消息到聊天界面
        addUserMessage(message);
        messageInput.value = '';

        // 禁用发送按钮防止重复点击
        sendButton.disabled = true;

        // 添加AI消息元素（初始为空）
        const aiMessageElement = addAiMessage('');
        const contentElement = aiMessageElement.querySelector('.message-content');

        // 准备请求参数
        const requestData = {
            question: message,
            model: modelSelect.value,
            enableSearch: webSearchCheckbox.checked,
            enableThinking: thinkingCheckbox.checked,
            rag: knowledgeBaseSelect.value || undefined,
            autoRag: autoRagCheckbox.checked
        };
        
        // 如果存在会话ID，则添加到请求中
        if (conversationId) {
            requestData.conversationId = conversationId;
        }

        // 发起流式请求
        fetch('http://localhost:9090/chat/chat-stream', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(requestData)
        })
        .then(response => {
            if (!response.body) {
                throw new Error('ReadableStream not supported in this browser.');
            }

            const reader = response.body.getReader();
            const decoder = new TextDecoder();

            // 处理流式响应
            function readStream() {
                reader.read().then(({ done, value }) => {
                    if (done) {
                        sendButton.disabled = false;
                        return;
                    }

                    const chunk = decoder.decode(value, { stream: true });
                    
                    // 将新的块添加到缓冲区
                    buffer += chunk;
                    
                    // 按行分割缓冲区
                    const lines = buffer.split('\n');
                    
                    // 保留最后一个可能不完整的行在缓冲区中
                    buffer = lines.pop();
                    
                    // 处理每一行
                    for (const line of lines) {
                        if (line.startsWith('data: ')) {
                            const data = line.slice(6); // 移除 'data: ' 前缀
                            
                            try {
                                console.log('Received data:', data);
                                const jsonData = JSON.parse(data);
                                
                                // 根据返回数据的类型进行处理
                                if (jsonData.type === "CONTENT") {
                                    // 更新消息内容
                                    const currentContent = contentElement.textContent;
                                    contentElement.textContent = currentContent + jsonData.data;
                                    
                                    // 滚动到底部
                                    chatMessages.scrollTop = chatMessages.scrollHeight;
                                } else if (jsonData.type === "THINKING" && thinkingCheckbox.checked) {
                                    // 显示思考过程
                                    addThinkingMessage(jsonData.data);
                                } else if (jsonData.type === "SEARCH_RESULT" && webSearchCheckbox.checked) {
                                    // 显示搜索结果
                                    addSearchResultMessage(jsonData.data);
                                } else if (jsonData.type === "TITLE") {
                                    // 处理标题消息（可以用于更新页面标题或会话标题）
                                    console.log("会话标题:", jsonData.data);
                                } else if (jsonData.type === "CONVERSATION_ID") {
                                    // 保存会话ID
                                    conversationId = jsonData.data;
                                    console.log("会话ID已保存:", conversationId);
                                } else if (jsonData.type === "END") {
                                    // 流结束消息
                                    console.log("流式传输完成");
                                    // 移除光标动画
                                    contentElement.classList.remove('streaming-text');
                                } else if (jsonData.type === "ERROR") {
                                    // 显示错误信息
                                    addErrorMessage(jsonData.data);
                                    sendButton.disabled = false;
                                    reader.cancel();
                                    return;
                                }
                            } catch (e) {
                                console.error('Error parsing JSON:', e);
                                console.log('Failed to parse data:', data);
                            }
                        }
                    }

                    // 继续读取流
                    readStream();
                }).catch(error => {
                    console.error('Error reading stream:', error);
                    sendButton.disabled = false;
                    addErrorMessage('抱歉，接收消息时出现错误，请稍后重试。');
                });
            }

            // 开始读取流
            readStream();
        })
        .catch(error => {
            console.error('Error sending message:', error);
            sendButton.disabled = false;
            addErrorMessage('抱歉，发送消息时出现错误，请稍后重试。');
        });
    }

    // 添加用户消息
    function addUserMessage(content) {
        const messageElement = document.createElement('div');
        messageElement.className = 'message user-message';
        messageElement.innerHTML = `
            <div class="message-content">${content}</div>
        `;
        chatMessages.appendChild(messageElement);
        chatMessages.scrollTop = chatMessages.scrollHeight;
        return messageElement;
    }

    // 添加AI消息
    function addAiMessage(content) {
        const messageElement = document.createElement('div');
        messageElement.className = 'message ai-message';
        messageElement.innerHTML = `
            <div class="message-content streaming-text">${content}</div>
        `;
        chatMessages.appendChild(messageElement);
        chatMessages.scrollTop = chatMessages.scrollHeight;
        return messageElement;
    }

    // 添加思考中消息
    function addThinkingMessage(content) {
        // 检查是否已经存在思考消息元素，如果存在则更新内容
        let thinkingElement = document.querySelector('.thinking-message:last-child');
        if (!thinkingElement) {
            thinkingElement = document.createElement('div');
            thinkingElement.className = 'message thinking-message';
            thinkingElement.innerHTML = `
                <div class="message-content"></div>
            `;
            chatMessages.appendChild(thinkingElement);
        }
        
        const contentElement = thinkingElement.querySelector('.message-content');
        contentElement.innerHTML = `<strong>思考中:</strong> ${content}`;
        chatMessages.scrollTop = chatMessages.scrollHeight;
        return thinkingElement;
    }

    // 添加搜索结果消息
    function addSearchResultMessage(content) {
        const messageElement = document.createElement('div');
        messageElement.className = 'message thinking-message';
        messageElement.innerHTML = `
            <div class="message-content"><strong>搜索结果:</strong> ${content}</div>
        `;
        chatMessages.appendChild(messageElement);
        chatMessages.scrollTop = chatMessages.scrollHeight;
        return messageElement;
    }

    // 添加错误消息
    function addErrorMessage(content) {
        const messageElement = document.createElement('div');
        messageElement.className = 'message ai-message';
        messageElement.innerHTML = `
            <div class="message-content" style="color: red;"><strong>错误:</strong> ${content}</div>
        `;
        chatMessages.appendChild(messageElement);
        chatMessages.scrollTop = chatMessages.scrollHeight;
        return messageElement;
    }
});