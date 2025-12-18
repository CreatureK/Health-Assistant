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

        // HTTP 层错误
        if (status >= 400) {
          if (status === 401) {
            uni.removeStorageSync("token");
            uni.showToast({ title: "请先登录", icon: "none" });
            gotoLoginOnce();
          } else {
            uni.showToast({ title: body?.msg || `请求失败(${status})`, icon: "none" });
          }
          return reject({ statusCode: status, body });
        }

        // 业务层：{ code, msg, data }
        if (body && typeof body === "object" && "code" in body) {
          if (body.code === 0) return resolve(body.data);
          uni.showToast({ title: body.msg || "请求失败", icon: "none" });
          return reject(body);
        }

        // 兼容旧接口：直接返回 body
        resolve(body);
      },
      fail(err) {
        uni.showToast({ title: "网络异常", icon: "none" });
        reject(err);
      }
    });
  });
}

export { API } from "./api";
