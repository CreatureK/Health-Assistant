<<<<<<< HEAD
// 优先读环境变量（配合你们的 .env.example）
// - vue-cli/uniapp 常用：VUE_APP_BASE_URL
// - Vite(若你们是vite项目)：VITE_BASE_URL
// export const BASE_URL =
//   (typeof process !== "undefined" &&
//     process.env &&
//     (process.env.VUE_APP_BASE_URL || process.env.VITE_BASE_URL)) ||
//   "http://192.168.1.107:8080";
const BASE_URL = 'http://localhost:8080/api/v1';
=======
export const BASE_URL = "http://localhost:8080";
>>>>>>> origin/master

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
<<<<<<< HEAD
 * - 再按接口契约判定 body.code 是否成功（接口文档统一响应 {code,msg,data}） :contentReference[oaicite:7]{index=7}
=======
 * - 再按接口契约判定 body.code 是否成功（接口文档统一响应 {code,msg,data}）
>>>>>>> origin/master
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

<<<<<<< HEAD
        // 1) HTTP 非 2xx
=======
        // 1) HTTP 非 2xx：按你原来的分类处理
>>>>>>> origin/master
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

<<<<<<< HEAD
        // 2) HTTP 2xx：按 body.code 判断
        if (body && typeof body === "object" && "code" in body) {
          if (body.code === 200) return resolve(body.data);

=======
        // 2) HTTP 2xx：再按接口文档的 body.code 判断
        // 文档：统一响应 {code:200,msg:'ok',data:<payload>}
        if (body && typeof body === "object" && "code" in body) {
          if (body.code === 200) {
            return resolve(body.data);
          }

          // code != 200 也算失败（哪怕 HTTP 200）
>>>>>>> origin/master
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

<<<<<<< HEAD
        // 3) 兼容：未包 code 的响应
=======
        // 3) 兼容：如果后端某些接口没包 code（极少数），就直接返回 body
>>>>>>> origin/master
        return resolve(body);
      },

      fail(err) {
        uni.showToast({ title: "网络异常", icon: "none" });
        reject(err);
      }
    });
  });
}
<<<<<<< HEAD
export {API} from "./api";
=======
>>>>>>> master
