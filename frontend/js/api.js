(function () {
  const request = async (path, options = {}) => {
    const baseUrl = window.SCHEL_CONFIG && window.SCHEL_CONFIG.apiBaseUrl;
    if (!baseUrl) {
      throw new Error('Backend API base URL is not configured.');
    }

    const response = await fetch(`${baseUrl}${path}`, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...(options.headers || {})
      }
    });

    const text = await response.text();
    let payload = null;

    if (text) {
      try {
        payload = JSON.parse(text);
      } catch {
        if (response.ok) {
          throw new Error('Backend returned an invalid JSON response.');
        }
        payload = { message: text };
      }
    }

    if (!response.ok) {
      const message = payload && (payload.error || payload.message);
      throw new Error(message || `Request failed with status ${response.status}.`);
    }

    return payload;
  };

  const jsonRequest = (method, data) => ({
    method,
    body: JSON.stringify(data)
  });

  window.SCHEL = window.SCHEL || {};
  window.SCHEL.api = {
    request,
    checkHealth: () => request('/api/health'),

    register: (username, email, password) => request(
      '/api/auth/register',
      jsonRequest('POST', { username, email, password })
    ),
    login: (identifier, password) => request(
      '/api/auth/login',
      jsonRequest('POST', { identifier, password })
    ),
    getCurrentUser: () => request('/api/auth/me'),
    logout: () => request('/api/auth/logout', { method: 'POST' }),

    getCategories: () => request('/api/categories'),
    createCategory: (data) => request('/api/categories', jsonRequest('POST', data)),
    updateCategory: (id, data) => request(`/api/categories/${encodeURIComponent(id)}`, jsonRequest('PATCH', data)),
    deleteCategory: (id) => request(`/api/categories/${encodeURIComponent(id)}`, { method: 'DELETE' }),

    getSchedules: () => request('/api/schedules'),
    getSchedule: (id) => request(`/api/schedules/${encodeURIComponent(id)}`),
    createSchedule: (data) => request('/api/schedules', jsonRequest('POST', data)),
    updateSchedule: (id, data) => request(`/api/schedules/${encodeURIComponent(id)}`, jsonRequest('PATCH', data)),
    deleteSchedule: (id) => request(`/api/schedules/${encodeURIComponent(id)}`, { method: 'DELETE' }),

    getReminders: () => request('/api/reminders'),
    createReminder: (data) => request('/api/reminders', jsonRequest('POST', data)),
    updateReminder: (id, data) => request(`/api/reminders/${encodeURIComponent(id)}`, jsonRequest('PATCH', data)),
    deleteReminder: (id) => request(`/api/reminders/${encodeURIComponent(id)}`, { method: 'DELETE' })
  };

  window.SCHEL.apiRequest = request;
})();