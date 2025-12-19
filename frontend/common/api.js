/**
 * ================== 接口契约（给后端同学）==================
 * GET  /api/v1/auth/captcha
 * POST /api/v1/auth/login
 *
 * ------------------ 用药管理（新增） -------------------------
 * GET    /api/v1/med/plans
 * POST   /api/v1/med/plans
 * GET    /api/v1/med/plans/:id
 * PUT    /api/v1/med/plans/:id
 * DELETE /api/v1/med/plans/:id
 * POST   /api/v1/med/plans/:id/remind          body: {remindEnabled}
 *
 * GET    /api/v1/med/today                     query: {date}
 *
 * POST   /api/v1/med/records/:recordId/mark    body: {status} taken|missed
 * POST   /api/v1/med/records/:recordId/adjust  body: {status, actionAt?, note?}
 * GET    /api/v1/med/records                   query: {planId?, status?, startDate?, endDate?, id?}
 *
 * ------------------ 药品库（新增） ---------------------------
 * GET    /api/v1/med/drugs                     query: {keyword?, page?, size?}
 * GET    /api/v1/med/drugs/:id
 *
 * GET    /api/v1/wechat/subscribe/config       -> {templateIds:[]}
 * POST   /api/v1/wechat/subscribe/report       body: {granted:boolean, detail?:object}
 * ============================================================
 */
export const API = {
  // auth
  captcha: "/api/v1/auth/captcha",
  login: "/api/v1/auth/login",
  register: "/api/v1/auth/register",

  // med plans
  medPlans: "/api/v1/med/plans", // GET/POST
  medPlanDetail: (id) => `/api/v1/med/plans/${id}`, // GET/PUT/DELETE
  medPlanRemind: (id) => `/api/v1/med/plans/${id}/remind`, // POST

  // today
  medToday: "/api/v1/med/today", // GET

  // records
  medRecords: "/api/v1/med/records", // GET
  medRecordMark: (recordId) => `/api/v1/med/records/${recordId}/mark`, // POST
  medRecordAdjust: (recordId) => `/api/v1/med/records/${recordId}/adjust`, // POST

  // drugs
  medDrugs: "/api/v1/med/drugs", // GET
  medDrugDetail: (id) => `/api/v1/med/drugs/${id}`, // GET

  // wechat subscribe
  wechatSubscribeConfig: "/api/v1/wechat/subscribe/config", // GET
  wechatSubscribeReport: "/api/v1/wechat/subscribe/report" // POST
};
