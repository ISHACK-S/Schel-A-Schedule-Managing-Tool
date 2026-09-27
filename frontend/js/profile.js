document.addEventListener('DOMContentLoaded', () => {
  const logoutButton = document.querySelector('[data-action="logout"]');

  if (logoutButton) {
    logoutButton.addEventListener('click', () => {
      window.SCHEL.logout();
    });
  }
});
