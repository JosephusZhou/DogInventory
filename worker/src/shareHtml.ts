export function renderLandingPage(shareId: string, publicBaseUrl: string): string {
  const escaped = shareId.replace(/[^A-Za-z0-9]/g, "");
  // 与 Android Manifest 的 doginv 过滤器保持一致：scheme=doginv, host=share.com, pathPrefix=/s/
  const doginvUrl = `doginv://share.com/s/${escaped}`;
  const playStoreUrl = "https://play.google.com/store/apps/details?id=com.doginventory";
  return `<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover" />
  <meta name="theme-color" content="#FF9B71" />
  <title>线条小狗存货 · 共享存货</title>
  <style>
    *,*::before,*::after { box-sizing: border-box; }
    html, body { margin: 0; padding: 0; height: 100%; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif;
      background: linear-gradient(160deg, #FFF8F0 0%, #FFEFE0 100%);
      color: #2A2A2A;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 24px;
      min-height: 100%;
    }
    .card {
      max-width: 420px;
      width: 100%;
      background: #FFFFFF;
      border-radius: 20px;
      box-shadow: 0 12px 32px rgba(255, 107, 138, 0.12);
      padding: 32px 24px;
      text-align: center;
    }
    .badge {
      display: inline-block;
      background: rgba(255, 155, 113, 0.18);
      color: #C95E2E;
      font-size: 13px;
      font-weight: 600;
      padding: 4px 10px;
      border-radius: 999px;
      margin-bottom: 16px;
    }
    h1 {
      font-size: 22px;
      margin: 0 0 8px;
      color: #2A2A2A;
    }
    p {
      font-size: 14px;
      line-height: 1.6;
      color: #6B6B6B;
      margin: 0 0 24px;
    }
    .open-btn {
      display: inline-block;
      width: 100%;
      background: #FF9B71;
      color: #FFFFFF;
      font-size: 16px;
      font-weight: 600;
      padding: 14px 20px;
      border-radius: 12px;
      text-decoration: none;
      transition: background 0.15s ease;
    }
    .open-btn:hover, .open-btn:active { background: #F58B5C; }
    .fallback-text {
      margin: 16px 0 0;
      font-size: 13px;
      color: #8A8A8A;
    }
    .fallback-text a {
      color: #FF6B8A;
      text-decoration: none;
    }
  </style>
</head>
<body>
  <div class="card">
    <div class="badge">线条小狗存货</div>
    <h1>有人想和你分享一份存货清单</h1>
    <p>点击下方按钮，在 App 中查看并选择要导入到本机的存货。</p>
    <a id="open" class="open-btn" href="${doginvUrl}">在 App 中打开</a>
    <p class="fallback-text">未安装 App？<a href="${playStoreUrl}">前往下载</a></p>
  </div>
  <script>
    (function () {
      // 桌面浏览器不自动唤起（会污染地址栏）
      var isMobile = /Android|iPhone|iPad|iPod/i.test(navigator.userAgent);
      if (!isMobile) return;

      // 移动端页面加载完成后，用隐藏 iframe 尝试唤起 App
      // iframe 导航对自定义协议是常见且稳定的方案；window.location 在部分浏览器会被拦截
      var doginvUrl = ${JSON.stringify(doginvUrl)};
      var iframe = document.createElement('iframe');
      iframe.style.display = 'none';
      iframe.src = doginvUrl;
      document.body.appendChild(iframe);

      // App 接管后页面会 hidden，无需清理 UI；用户留在页面说明 App 未安装/未响应，按钮和下载链接依然可用
    })();
  </script>
</body>
</html>`;
}
