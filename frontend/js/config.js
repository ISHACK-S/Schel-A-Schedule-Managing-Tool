window.SCHEL_CONFIG = {
  appName: 'SCHEL',
  apiBaseUrl: 'http://localhost:8080',
  defaultRoute: 'login.html',
  authTokenKey: 'schel_auth_session',
  authMode: 'backend-auth'
};

window.SCHEL = {
  isAuthenticated: function () {
    const raw = localStorage.getItem(window.SCHEL_CONFIG.authTokenKey);
    if (!raw) return false;

    try {
      const session = JSON.parse(raw);
      return Boolean(session && session.user && session.user.id);
    } catch (error) {
      return false;
    }
  },

  getCurrentUser: function () {
    const raw = localStorage.getItem(window.SCHEL_CONFIG.authTokenKey);
    if (!raw) return null;

    try {
      return JSON.parse(raw).user || null;
    } catch (error) {
      return null;
    }
  },

  setCurrentUser: function (user) {
    if (!user || !user.id) return false;

    const payload = {
      user: {
        id: user.id,
        username: user.username || '',
        email: user.email || '',
        created_at: user.created_at || user.createdAt || null
      },
      updatedAt: new Date().toISOString()
    };

    localStorage.setItem(window.SCHEL_CONFIG.authTokenKey, JSON.stringify(payload));
    return true;
  },

  clearSession: function () {
    localStorage.removeItem(window.SCHEL_CONFIG.authTokenKey);
    this.clearUserData();
  },

  clearUserData: function () {
    const storageKeys = [
      'schel_schedule_collection',
      'schel_category_collection',
      'schel_reminder_collection',
      'schel_dashboard_cache',
      'schel_profile_cache'
    ];

    storageKeys.forEach((key) => localStorage.removeItem(key));
  },

  logout: async function () {
    try {
      await window.SCHEL.api.logout();
    } catch (error) {
      window.alert(error?.message || 'Unable to log out. Please try again.');
      return false;
    }

    this.clearSession();
    window.location.href = 'login.html';
    return true;
  },

  requireAuth: function () {
    if (!this.isAuthenticated()) {
      window.location.href = 'login.html';
      return false;
    }
    return true;
  },

  navigate: function (path) {
    if (!path) return;
    window.location.href = path;
  },

  scheduleApi: {
    normalizePriority: function (value) {
      const normalized = (value || '').toString().trim().toUpperCase();
      return ['LOW', 'MEDIUM', 'HIGH'].includes(normalized) ? normalized : 'MEDIUM';
    },

    normalizeStatus: function (value) {
      const normalized = (value || '').toString().trim().toUpperCase();
      return ['PENDING', 'COMPLETED', 'MISSED'].includes(normalized) ? normalized : 'PENDING';
    },

    normalizeSchedule: function (value) {
      const item = value || {};
      return {
        id: item.id || item.schedule_id || null,
        user_id: item.user_id || item.userId || null,
        category_id: item.category_id || item.categoryId || null,
        title: item.title || 'Untitled schedule',
        description: item.description || '',
        task_date: item.task_date || item.taskDate || '',
        start_time: item.start_time || item.startTime || '',
        end_time: item.end_time || item.endTime || '',
        priority: this.normalizePriority(item.priority),
        status: this.normalizeStatus(item.status),
        category: item.category || 'General',
        created_at: item.created_at || item.createdAt || null,
        updated_at: item.updated_at || item.updatedAt || null
      };
    },

    getSchedules: async function () {
      const result = await window.SCHEL.apiRequest('/api/schedules', { method: 'GET' });
      if (!Array.isArray(result?.schedules)) {
        throw new Error('The server returned an invalid schedules response.');
      }
      return result.schedules.map((item) => this.normalizeSchedule(item));
    },

    createSchedule: async function (payload) {
      const safePayload = {
        title: payload.title,
        description: payload.description || '',
        task_date: payload.task_date,
        start_time: payload.start_time,
        end_time: payload.end_time,
        priority: this.normalizePriority(payload.priority),
        status: this.normalizeStatus(payload.status),
        category_id: payload.category_id || null
      };

      const result = await window.SCHEL.apiRequest('/api/schedules', {
        method: 'POST',
        body: JSON.stringify(safePayload)
      });
      if (!result?.schedule) {
        throw new Error('The server did not return the created schedule.');
      }
      return this.normalizeSchedule(result.schedule);
    },

    updateSchedule: async function (payload) {
      const id = payload && (payload.id || payload.schedule_id);
      if (!id) {
        throw new Error('Schedule id is required.');
      }

      const safePayload = {
        title: payload.title,
        description: payload.description || '',
        task_date: payload.task_date,
        start_time: payload.start_time,
        end_time: payload.end_time,
        priority: this.normalizePriority(payload.priority),
        status: this.normalizeStatus(payload.status),
        category_id: payload.category_id || null
      };

      const result = await window.SCHEL.apiRequest(`/api/schedules/${id}`, {
        method: 'PATCH',
        body: JSON.stringify(safePayload)
      });
      if (!result?.schedule) {
        throw new Error('The server did not return the updated schedule.');
      }
      return this.normalizeSchedule(result.schedule);
    },

    deleteSchedule: async function (id) {
      await window.SCHEL.apiRequest(`/api/schedules/${id}`, { method: 'DELETE' });
      return true;
    }
  },

};
