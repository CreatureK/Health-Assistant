export const BASE_URL = "http://localhost:8080";

function getToken() {
  return uni.getStorageSync("token") || "";
}

function gotoLoginOnce() {
  const pages = getCurrentPages();
  const cur = pages?.[pages.length - 1]?.route || "";
  if (cur !== "pages/login/login") {
    uni.reLaunch({ url: "/pages/login/login" });
  }
}

/**
 * 统一请求封装
 * - 先按 HTTP 状态码判定是否“传输成功”
 * - 再按接口契约判定 body.code 是否成功（接口文档统一响应 {code,msg,data}）
 *
 * 成功：HTTP 2xx 且 (body.code 不存在 或 body.code === 200)
 * 失败：HTTP 非 2xx 或 body.code 存在且 !== 200
 */
export function request({ url, method = "GET", data, header }) {
  return new Promise((resolve, reject) => {
    const token = getToken();

    uni.request({
      url: BASE_URL + url,
      method,
      data,
      header: {
        "content-type": "application/json",
        ...(header || {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },

      success(res) {
        const status = res.statusCode;
        const body = res.data;

        // 1) HTTP 非 2xx：按你原来的分类处理
        if (!(status >= 200 && status < 300)) {
          if (status === 401) {
            uni.removeStorageSync("token");
            uni.showToast({ title: "请先登录", icon: "none" });
            gotoLoginOnce();
            return reject({ statusCode: status, body });
          }

          if (status >= 400 && status < 500) {
            uni.showToast({
              title: body?.msg || body?.message || `请求错误(${status})`,
              icon: "none"
            });
            return reject({ statusCode: status, body });
          }

          if (status >= 500 && status < 600) {
            uni.showToast({
              title: body?.msg || body?.message || `服务异常(${status})`,
              icon: "none"
            });
            return reject({ statusCode: status, body });
          }

          uni.showToast({
            title: body?.msg || body?.message || `请求失败(${status})`,
            icon: "none"
          });
          return reject({ statusCode: status, body });
        }

        // 2) HTTP 2xx：再按接口文档的 body.code 判断
        // 文档：统一响应 {code:200,msg:'ok',data:<payload>}
        if (body && typeof body === "object" && "code" in body) {
          if (body.code === 200) {
            return resolve(body.data);
          }

          // code != 200 也算失败（哪怕 HTTP 200）
          if (body.code === 401) {
            uni.removeStorageSync("token");
            uni.showToast({ title: body?.msg || "请先登录", icon: "none" });
            gotoLoginOnce();
            return reject({ statusCode: status, body });
          }

          uni.showToast({
            title: body?.msg || `请求失败(code=${body.code})`,
            icon: "none"
          });
          return reject({ statusCode: status, body });
        }

        // 3) 兼容：如果后端某些接口没包 code（极少数），就直接返回 body
        return resolve(body);
      },

      fail(err) {
        uni.showToast({ title: "网络异常", icon: "none" });
        reject(err);
      }
    });
  });
}
