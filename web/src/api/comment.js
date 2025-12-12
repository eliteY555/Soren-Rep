import request from '@/utils/request'
import { isRequestCanceled } from '@/utils/request'

// 发布评论
export function publishComment(data) {
  return request({
    url: '/comment',
    method: 'post',
    data
  }).catch(error => {
    if (isRequestCanceled(error)) {
      return Promise.reject(error);
    }
    console.error('发布评论失败:', error);
    return Promise.reject(error);
  });
}

// 获取评论列表
export function getCommentList(recordId) {
  const requestConfig = recordId 
    ? { url: `/comment/${recordId}`, method: 'get' }
    : { url: `/comment`, method: 'get' };
    
  return request(requestConfig).catch(error => {
    if (isRequestCanceled(error)) {
      return Promise.reject(error);
    }
    console.error('获取评论列表失败:', error);
    return Promise.reject(error);
  });
}

// 删除评论
export function deleteComment(commentId) {
  return request({
    url: `/comment/delete/${commentId}`,
    method: 'get',
  }).catch(error => {
    if (isRequestCanceled(error)) {
      return Promise.reject(error);
    }
    console.error('删除评论失败:', error);
    return Promise.reject(error);
  });
}

// 回复评论
export function replyComment(data) {
  return request({
    url: '/reply',
    method: 'post',
    data
  }).catch(error => {
    if (isRequestCanceled(error)) {
      return Promise.reject(error);
    }
    console.error('回复评论失败:', error);
    return Promise.reject(error);
  });
}

// 获取回复列表
export function getReplyList(commentId) {
  return request({
    url: `/reply/${commentId}`,
    method: 'get',
  }).catch(error => {
    if (isRequestCanceled(error)) {
      return Promise.reject(error);
    }
    console.error('获取回复列表失败:', error);
    return Promise.reject(error);
  });
}

// 删除回复
export function deleteReply(replyId) {
  return request({
    url: `/reply/delete/${replyId}`,
    method: 'get',
  }).catch(error => {
    if (isRequestCanceled(error)) {
      return Promise.reject(error);
    }
    console.error('删除回复失败:', error);
    return Promise.reject(error);
  });
}
