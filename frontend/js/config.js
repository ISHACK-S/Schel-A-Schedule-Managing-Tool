window.SCHEL_CONFIG = {
  appName: 'SCHEL',
  apiBaseUrl: 'http://localhost:8080',
  defaultRoute: 'login.html',
  authTokenKey: 'schel_auth_session',
  authMode: 'backend-auth'
};

window.SCHEL = {
  apiRequest: async function (path, options = {}) {
    const baseUrl = (window.SCHEL_CONFIG && window.SCHEL_CONFIG.apiBaseUrl) || 'http://localhost:8080';
    const response = await fetch(`${baseUrl}${path}`, {
      headers: {
        'Content-Type': 'application/json',
        ...(options.headers || {})
      },
      ...options
    });

    const text = await response.text();
    let payload = null;

    if (text) {
      try {
        payload = JSON.parse(text);
      } catch (error) {
        payload = { message: text };
      }
    }

    if (!response.ok) {
      const message = payload && payload.error ? payload.error : payload && payload.message ? payload.message : 'Request failed.';
      throw new Error(message);
    }

    return payload;
  },

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

  logout: function () {
    localStorage.removeItem(window.SCHEL_CONFIG.authTokenKey);
    this.clearUserData();
    window.location.href = 'login.html';
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
      const user = window.SCHEL.getCurrentUser();
      if (!user) return [];

      const result = await window.SCHEL.apiRequest('/api/schedules', { method: 'GET' });
      const items = Array.isArray(result && result.schedules) ? result.schedules : [];
      return items.map((item) => this.normalizeSchedule(item));
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
      return this.normalizeSchedule(result && result.schedule ? result.schedule : safePayload);
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
      return this.normalizeSchedule(result && result.schedule ? result.schedule : { ...safePayload, id });
    },

    deleteSchedule: async function (id) {
      await window.SCHEL.apiRequest(`/api/schedules/${id}`, { method: 'DELETE' });
      return true;
    }
  },

  categoryApi: {
    normalizeCategory: function (value) {
      const item = value || {};
      const rawColor = (item.color || '#5b6cff').trim();
      const isValidHex = /^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$/.test(rawColor);

      return {
        id: item.id || null,
        user_id: item.user_id || null,
        name: (item.name || 'Untitled category').trim(),
        color: isValidHex ? rawColor : '#5b6cff',
        created_at: item.created_at || item.createdAt || null
      };
    },

    getCategories: async function () {
      const user = window.SCHEL.getCurrentUser();
      if (!user) return [];

      const result = await window.SCHEL.apiRequest('/api/categories', { method: 'GET' });
      const items = Array.isArray(result && result.categories) ? result.categories : [];
      return items.map((item) => this.normalizeCategory(item));
    },

    createCategory: async function (payload) {
      const safePayload = {
        name: payload.name,
        color: payload.color || '#5b6cff'
      };

      const result = await window.SCHEL.apiRequest('/api/categories', {
        method: 'POST',
        body: JSON.stringify(safePayload)
      });

      const category = result && result.category ? result.category : null;
      return this.normalizeCategory(category || { ...safePayload });
    },

    updateCategory: async function (payload) {
      const id = payload && payload.id;
      if (!id) {
        throw new Error('Category id is required.');
      }

      const safePayload = {
        name: payload.name,
        color: payload.color || '#5b6cff'
      };

      const result = await window.SCHEL.apiRequest(`/api/categories/${id}`, {
        method: 'PATCH',
        body: JSON.stringify(safePayload)
      });

      const category = result && result.category ? result.category : { ...safePayload, id };
      return this.normalizeCategory(category);
    },

    deleteCategory: async function (id) {
      await window.SCHEL.apiRequest(`/api/categories/${id}`, { method: 'DELETE' });
      return true;
    }
  },

  reminderApi: {
    normalizeReminder: function (value) {
      const item = value || {};
      const scheduleId = item.schedule_id || item.scheduleId || null;
      const timeValue = item.reminder_time || item.reminderTime || item.datetime || null;

      return {
        id: item.id || item.reminder_id || null,
        schedule_id: scheduleId,
        reminder_time: timeValue,
        is_sent: Boolean(item.is_sent ?? item.isSent ?? item.sent ?? false)
      };
    },

    getSchedules: async function () {
      return window.SCHEL.scheduleApi.getSchedules();
    },

    toReminderDateTime: function (value) {
      if (!value) return null;
      const date = new Date(value);
      if (Number.isNaN(date.getTime())) {
        return null;
      }
      return date.toISOString();
    },

    getReminders: async function () {
      const user = window.SCHEL.getCurrentUser();
      if (!user) return [];

      const result = await window.SCHEL.apiRequest('/api/reminders', { method: 'GET' });
      const items = Array.isArray(result && result.reminders) ? result.reminders : [];
      return items.map((item) => this.normalizeReminder(item));
    },

    createReminder: async function (payload) {
      const scheduleId = payload && (payload.schedule_id || payload.scheduleId);
      const reminderTime = this.toReminderDateTime(payload && payload.reminder_time);

      if (!scheduleId) {
        throw new Error('Please select a schedule.');
      }
      if (!reminderTime) {
        throw new Error('Please select a reminder date and time.');
      }

      const result = await window.SCHEL.apiRequest('/api/reminders', {
        method: 'POST',
        body: JSON.stringify({ schedule_id: scheduleId, reminder_time: reminderTime })
      });

      const reminder = result && result.reminder ? result.reminder : { schedule_id: scheduleId, reminder_time: reminderTime, is_sent: false };
      return this.normalizeReminder(reminder);
    },

    updateReminder: async function (payload) {
      const id = payload && (payload.id || payload.reminder_id);
      if (!id) {
        throw new Error('Reminder id is required.');
      }

      const reminderTime = this.toReminderDateTime(payload && payload.reminder_time);
      if (!reminderTime) {
        throw new Error('Please select a reminder date and time.');
      }

      const result = await window.SCHEL.apiRequest(`/api/reminders/${id}`, {
        method: 'PATCH',
        body: JSON.stringify({ reminder_time: reminderTime })
      });

      const reminder = result && result.reminder ? result.reminder : { id, reminder_time: reminderTime, is_sent: payload.is_sent || false };
      return this.normalizeReminder(reminder);
    },

    deleteReminder: async function (id) {
      await window.SCHEL.apiRequest(`/api/reminders/${id}`, { method: 'DELETE' });
      return true;
    }
  },

  authApi: {
    login: async function (payload) {
      const { identifier, password } = payload || {};

      if (!identifier || !password) {
        throw new Error('Invalid email or password.');
      }

      const result = await window.SCHEL.apiRequest('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify({ identifier, password })
      });

      if (!result || !result.user) {
        throw new Error('Unable to sign in.');
      }

      return result;
    },

    register: async function (payload) {
      const { username, email, password } = payload || {};

      if (!username || !email || !password) {
        throw new Error('Please complete all required fields.');
      }

      const result = await window.SCHEL.apiRequest('/api/auth/register', {
        method: 'POST',
        body: JSON.stringify({ username, email, password })
      });

      if (!result || !result.user) {
        throw new Error('Unable to register this account.');
      }

      return result;
    }
  }
};
