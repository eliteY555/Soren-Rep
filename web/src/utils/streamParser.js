/**
 * 流式响应解析器
 * 用于处理从服务器返回的流式文本响应
 */

/**
 * 解析流式响应数据
 * @param {Object|Blob|ReadableStream} response - Axios响应对象、Blob数据或可读流
 * @param {Function} onChunk - 每次收到数据块时的回调函数
 * @param {Function} onDone - 数据接收完成时的回调函数
 * @param {Function} onError - 发生错误时的回调函数
 * @returns {Promise} 解析完成的Promise
 */
export async function parseStream(response, { onChunk, onDone, onError }) {
  try {
    // 处理Axios响应对象
    if (response && response.data) {
      const stream = response.data;
      
      // 处理Blob类型的响应
      if (stream instanceof Blob) {
        const text = await stream.text();
        if (onChunk) onChunk(text, text);
        if (onDone) onDone(text);
        return text;
      }
      
      // 处理ReadableStream类型的响应
      if (stream.getReader) {
        const reader = stream.getReader();
        let receivedText = '';

        while (true) {
          const { done, value } = await reader.read();
          
          if (done) {
            if (onDone) onDone(receivedText);
            break;
          }
          
          // 将二进制数据转换为文本
          const chunk = new TextDecoder().decode(value);
          receivedText += chunk;
          
          // 调用回调函数处理数据块
          if (onChunk) onChunk(chunk, receivedText);
        }
        
        return receivedText;
      }
    } else if (response instanceof Blob) {
      // 直接处理Blob
      const text = await response.text();
      if (onChunk) onChunk(text, text);
      if (onDone) onDone(text);
      return text;
    } else if (response && response.getReader) {
      // 直接处理ReadableStream
      const reader = response.getReader();
      let receivedText = '';

      while (true) {
        const { done, value } = await reader.read();
        
        if (done) {
          if (onDone) onDone(receivedText);
          break;
        }
        
        // 将二进制数据转换为文本
        const chunk = new TextDecoder().decode(value);
        receivedText += chunk;
        
        // 调用回调函数处理数据块
        if (onChunk) onChunk(chunk, receivedText);
      }
      
      return receivedText;
    }
    
    // 未知类型的响应
    throw new Error('不支持的响应类型');
  } catch (error) {
    console.error('流解析错误:', error);
    if (onError) onError(error);
    throw error;
  }
}

/**
 * 从响应中提取memoryId
 * @param {String} text - 响应文本
 * @returns {Number|null} 提取的memoryId或null
 */
export function extractMemoryId(text) {
  try {
    // 尝试从响应中提取memoryId (假设格式为 "memoryId:123")
    const match = text.match(/memoryId:(\d+)/);
    if (match && match[1]) {
      return parseInt(match[1]);
    }
    return null;
  } catch (e) {
    console.error('提取memoryId失败:', e);
    return null;
  }
}

/**
 * 取消流式请求
 * @param {AbortController} controller - 中止控制器
 * @param {Reader} reader - 流读取器
 */
export function cancelStreamRequest(controller, reader) {
  if (controller) {
    try {
      controller.abort();
    } catch (e) {
      console.error('取消请求失败:', e);
    }
  }
  
  if (reader) {
    try {
      reader.cancel();
    } catch (e) {
      console.error('取消读取器失败:', e);
    }
  }
}

/**
 * 格式化消息内容，处理换行和列表
 * @param {String} content - 原始消息内容
 * @returns {String} 格式化后的HTML内容
 */
export function formatMessageContent(content) {
  if (!content) return '';
  
  // 将换行符转换为HTML换行
  let formatted = content.replace(/\n/g, '<br>');
  
  // 处理列表 (简单处理数字和点的列表)
  formatted = formatted.replace(/(\d+\.\s+[^\n<]+)/g, '<li>$1</li>');
  
  // 处理可能的Markdown样式的列表
  formatted = formatted.replace(/(-\s+[^\n<]+)/g, '<li>$1</li>');
  
  // 如果有列表项，添加ul标签
  if (formatted.includes('<li>')) {
    formatted = formatted.replace(/(<li>.*?<\/li>)/gs, '<ul>$1</ul>');
  }
  
  return formatted;
} 