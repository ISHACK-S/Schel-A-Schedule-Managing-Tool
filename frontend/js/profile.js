document.addEventListener('DOMContentLoaded', () => {
  if (!window.SCHEL.requireAuth()) {
    return;
  }

  const user = window.SCHEL.getCurrentUser();
  const loading = document.getElementById('profile-loading');
  const content = document.getElementById('profile-content');
  const avatar = document.getElementById('profile-avatar');
  const profileName = document.getElementById('profile-name');
  const usernameInput = document.getElementById('profile-username');
  const emailInput = document.getElementById('profile-email');
  const createdAtInput = document.getElementById('profile-created-at');

  const formatDate = (value) => {
    if (!value) return 'Not available';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return 'Not available';
    return new Intl.DateTimeFormat('en', { month: 'short', day: 'numeric', year: 'numeric' }).format(date);
  };

  const getInitials = (name) => {
    const source = (name || '').trim();
    if (!source) return 'U';
    return source
      .split(/\s+/)
      .slice(0, 2)
      .map((part) => part.charAt(0).toUpperCase())
      .join('') || 'U';
  };

  const renderProfile = () => {
    if (!user) {
      loading.hidden = true;
      content.hidden = false;
      profileName.textContent = 'Unable to load your profile';
      usernameInput.value = 'Unavailable';
      emailInput.value = 'Unavailable';
      createdAtInput.value = 'Unavailable';
      return;
    }

    const username = user.username || 'Unknown user';
    const email = user.email || 'Not provided';
    const createdAt = user.created_at || user.createdAt || null;

    avatar.textContent = getInitials(username);
    profileName.textContent = username;
    usernameInput.value = username;
    emailInput.value = email;
    createdAtInput.value = formatDate(createdAt);

    loading.hidden = true;
    content.hidden = false;
  };

  const logoutButton = document.querySelector('[data-action="logout"]');
  if (logoutButton) {
    logoutButton.addEventListener('click', () => {
      window.SCHEL.logout();
    });
  }

  window.addEventListener('pageshow', (event) => {
    if (!window.SCHEL.isAuthenticated()) {
      window.location.href = 'login.html';
    }
  });

  renderProfile();
});
