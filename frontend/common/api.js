/**
 * ================== API 列表 ==================
 * 统一响应：{ code:200, msg:"ok", data:<payload> }  :contentReference[oaicite:4]{index=4}
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
<<<<<<< HEAD
 * 第二阶段：AI 对话 & 健康文章（你后端已做） :contentReference[oaicite:5]{index=5} :contentReference[oaicite:6]{index=6}
 * POST       /api/v1/ai/chat
 * GET        /api/v1/ai/sessions
 * GET        /api/v1/ai/sessions/:id/messages
 *
 * GET        /api/v1/articles
 * GET        /api/v1/articles/:id
 * ==============================================
=======
 * ------------------ 药品库（新增） ---------------------------
 * GET    /api/v1/med/drugs                     query: {keyword?, page?, size?}
 * GET    /api/v1/med/drugs/:id
 *
 * GET    /api/v1/wechat/subscribe/config       -> {templateIds:[]}
 * POST   /api/v1/wechat/subscribe/report       body: {granted:boolean, detail?:object}
 * ============================================================
>>>>>>> origin/master
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

  // drugs
  medDrugs: "/api/v1/med/drugs", // GET
  medDrugDetail: (id) => `/api/v1/med/drugs/${id}`, // GET

  // wechat subscribe
<<<<<<< HEAD
  wechatSubscribeConfig: "/wechat/subscribe/config",
  wechatSubscribeReport: "/wechat/subscribe/report",

  // health articles
  articles: "/articles",
  articleDetail: (id) => `/articles/${id}`,

  // AI chat
  aiChat: "/ai/chat",
  aiSessions: "/ai/sessions",
  aiSessionMessages: (id) => `/ai/sessions/${id}/messages`
=======
  wechatSubscribeConfig: "/api/v1/wechat/subscribe/config", // GET
  wechatSubscribeReport: "/api/v1/wechat/subscribe/report" // POST
>>>>>>> origin/master
};
