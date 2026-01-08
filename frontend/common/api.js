/**
 * ================== API 列表 ==================
 * 统一响应（你自建后端）：{ code:200, msg:"ok", data:<payload> }
 *
 * 登录：
 * GET  /api/v1/auth/captcha
 * POST /api/v1/auth/login
 * POST /api/v1/auth/register
 *
 * 用药管理：
 * GET/POST   /api/v1/med/plans
 * GET/PUT/DELETE /api/v1/med/plans/:id
 * POST       /api/v1/med/plans/:id/remind
 * GET        /api/v1/med/today
 * GET        /api/v1/med/records
 * POST       /api/v1/med/records/:recordId/mark
 * POST       /api/v1/med/records/:recordId/adjust
 *
 * 药品库：
 * GET        /api/v1/med/drugs
 * GET        /api/v1/med/drugs/:id
 *
 * 订阅：
 * GET        /api/v1/wechat/subscribe/config
 * POST       /api/v1/wechat/subscribe/report
 *
 * AI（自建后端）：
 * POST       /api/v1/ai/chat-messages   
 * GET        /api/v1/ai/conversations   
 *
 * ================== Dify 官方 API（直连） ==================
 * POST /v1/chat-messages
 * GET  /v1/conversations
 * GET  /v1/messages
 * ===========================================================
 */

export const API = {
  // auth
  captcha: "/auth/captcha",
  login: "/auth/login",
  register: "/auth/register",

  // med plans
  medPlans: "/med/plans",
  medPlanDetail: (id) => `/med/plans/${id}`,
  medPlanRemind: (id) => `/med/plans/${id}/remind`,

  // today
  medToday: "/med/today",

  // records
  medRecords: "/med/records",
  medRecordMark: (recordId) => `/med/records/${recordId}/mark`,
  medRecordAdjust: (recordId) => `/med/records/${recordId}/adjust`,

  // drugs
  medDrugs: "/med/drugs",
  medDrugDetail: (id) => `/med/drugs/${id}`,

  // wechat subscribe
  wechatSubscribeConfig: "/wechat/subscribe/config",
  wechatSubscribeReport: "/wechat/subscribe/report",

  // health articles
  articles: "/articles",
  articleDetail: (id) => `/articles/${id}`,

  //  AI chat（新接口）
  aiChatMessages: "/ai/chat-messages",
  aiConversations: "/ai/conversations",
  aiMessages: "/ai/messages"
};

export const DIFY_API = {
  chatMessages: "/v1/chat-messages",
  conversations: "/v1/conversations",
  messages: "/v1/messages"
};
