const Api = {

  // ===== 认证相关 =====
  getAuthStatus() {
    return fetch('/auth-status').then(r => r.json())
  },

  login(username, password) {
    return fetch('/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    }).then(r => r.json())
  },

  logout() {
    return fetch('/logout', { method: 'POST' }).then(r => r.json())
  },

  // ===== 统一请求封装（401 自动弹登录框）=====
  _fetch(url, options) {
    return fetch(url, options).then(resp => {
      if (resp.status === 401) {
        // 触发自定义事件通知前端弹登录框
        window.dispatchEvent(new CustomEvent('auth-required'))
        return Promise.reject(new Error('未登录或登录已过期'))
      }
      return resp
    })
  },

  // ===== 业务 API =====
  getConfig() {
    return this._fetch('/config').then(r => r.json())
  },

  getLocalBooks() {
    return this._fetch('/local-books').then(r => r.json())
  },

  search(keyword) {
    return this._fetch(`/search/aggregated?kw=${encodeURIComponent(keyword)}`)
      .then(r => r.json())
  },

  downloadBook(params) {
    return this._fetch(`/book-fetch?${params.toString()}`)
  },

  deleteBook(filename) {
    return this._fetch(`/book-delete?filename=${encodeURIComponent(filename)}`).then(r => r.json())
  },

  getSuggestions(kw) {
    return this._fetch(`/suggestion?kw=${encodeURIComponent(kw)}`).then(r => r.json())
  },

  getSources() {
    return this._fetch('/sources').then(r => r.json())
  },

  checkSources() {
    return this._fetch('/sources/check').then(r => r.json())
  },

}
