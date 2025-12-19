export const BASE_URL = "http://192.168.1.23:8080"; // ✅ 只改这里

function getToken() {
  return uni.getStorageSync("token") || "";
}

function gotoLoginOnce() {
  // 避免频繁跳转
  const pages = getCurrentPages();
  const cur = pages?.[pages.length - 1]?.route || "";
  if (cur !== "pages/login/login") {
    uni.reLaunch({ url: "/pages/login/login" });
  }
}

/**
 * 统一请求封装（按 HTTP 状态码判定成功/失败）
 * ✅ 200-299：成功
 * ❌ 400-499：前端错误（参数/权限/未登录等）
 * ❌ 500-599：后端错误（服务异常）
 *
 * 注意：不再依赖 body.code === 0 来判断成功
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

        // ✅ 成功：只看 HTTP 2xx
        if (status >= 200 && status < 300) {
          // 兼容：后端如果返回 {data: ...} 就取 data，否则返回整个 body
          return resolve(body?.data ?? body);
        }

        // ❌ 失败：按 HTTP 状态码分类处理
        if (status === 401) {
          // 未登录/登录过期
          uni.removeStorageSync("token");
          uni.showToast({ title: "请先登录", icon: "none" });
          gotoLoginOnce();
          return reject({ statusCode: status, body });
        }

        // 400-499：前端错误（参数、权限、资源不存在等）
        if (status >= 400 && status < 500) {
          uni.showToast({
            title: body?.msg || body?.message || `请求错误(${status})`,
            icon: "none"
          });
          return reject({ statusCode: status, body });
        }

        // 500-599：后端错误
        if (status >= 500 && status < 600) {
          uni.showToast({
            title: body?.msg || body?.message || `服务异常(${status})`,
            icon: "none"
          });
          return reject({ statusCode: status, body });
        }

        // 其他非常规状态码兜底
        uni.showToast({
          title: body?.msg || body?.message || `请求失败(${status})`,
          icon: "none"
        });
        return reject({ statusCode: status, body });
      },

      fail(err) {
        uni.showToast({ title: "网络异常", icon: "none" });
        reject(err);
      }
    });
  });
}

export { API } from "./api";
