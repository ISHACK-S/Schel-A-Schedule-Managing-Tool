window.SCHEL_CONFIG = {
  appName: 'SCHEL',
  apiBaseUrl: '',
  defaultRoute: 'login.html',
  authTokenKey: 'schel_auth_session',
  authMode: 'frontend-only'
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
        email: user.email || ''
      },
      updatedAt: new Date().toISOString()
    };

    localStorage.setItem(window.SCHEL_CONFIG.authTokenKey, JSON.stringify(payload));
    return true;
  },

  logout: function () {
    localStorage.removeItem(window.SCHEL_CONFIG.authTokenKey);
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

  authApi: {
    login: async function (payload) {
      return new Promise((resolve, reject) => {
        const { identifier, password } = payload || {};

        if (!identifier || !password) {
          reject(new Error('Invalid email or password.'));
          return;
        }

        setTimeout(() => {
          resolve({
            user: {
              id: 'local-user-' + Date.now(),
              username: identifier.includes('@') ? identifier.split('@')[0] : identifier,
              email: identifier.includes('@') ? identifier : ''
            }
          });
        }, 600);
      });
    },

    register: async function (payload) {
      return new Promise((resolve, reject) => {
        const { username, email, password } = payload || {};

        if (!username || !email || !password) {
          reject(new Error('Please complete all required fields.'));
          return;
        }

        setTimeout(() => {
          resolve({
            user: {
              id: 'local-user-' + Date.now(),
              username,
              email
            }
          });
        }, 700);
      });
    }
  }
};
