document.addEventListener('DOMContentLoaded', () => {
  const dashboardUserName = document.getElementById('dashboard-user-name');
  const welcomeTag = document.getElementById('welcome-tag');
  const todaySchedule = document.getElementById('today-schedule');
  const upcomingSchedules = document.getElementById('upcoming-schedules');
  const reminderList = document.getElementById('reminder-list');
  const categoryList = document.getElementById('category-list');

  if (!window.SCHEL.requireAuth()) {
    return;
  }

  const user = window.SCHEL.getCurrentUser();
  const name = user?.username || user?.email || 'Friend';
  const greeting = (() => {
    const hour = new Date().getHours();
    if (hour < 12) return 'Good morning';
    if (hour < 18) return 'Good afternoon';
    return 'Good evening';
  })();

  if (dashboardUserName) {
    dashboardUserName.textContent = `${greeting}, ${name}`;
  }

  if (welcomeTag) {
    welcomeTag.textContent = greeting;
  }

  const setLoadingState = (target, rows = 3, variant = 'list') => {
    if (!target) return;
    target.innerHTML = Array.from({ length: rows }, (_, index) => {
      const height = variant === 'schedule' ? (index % 2 === 0 ? '72px' : '62px') : '52px';
      return `<div class="load-skeleton" style="height:${height};margin-bottom:10px;"></div>`;
    }).join('');
  };

  const emptyState = (title, message) => `
    <div class="empty-state">
      <strong>${title}</strong>
      <p>${message}</p>
    </div>
  `;

  const mockStats = {
    'total-schedules': 18,
    pending: 7,
    completed: 9,
    missed: 2,
    categories: 5,
    reminders: 4
  };

  Object.entries(mockStats).forEach(([key, value]) => {
    const node = document.querySelector(`[data-stat="${key}"]`);
    if (node) {
      node.textContent = value;
    }
  });

  setLoadingState(todaySchedule, 3, 'schedule');
  setLoadingState(upcomingSchedules, 2);
  setLoadingState(reminderList, 2);
  setLoadingState(categoryList, 1);

  setTimeout(() => {
    todaySchedule.innerHTML = [
      {
        time: '09:00',
        title: 'Product Planning Session',
        detail: '09:00 AM – 10:30 AM',
        priority: 'High',
        priorityClass: 'priority-high',
        status: 'Pending',
        statusClass: 'status-pending'
      },
      {
        time: '12:30',
        title: 'Design review',
        detail: '12:30 PM – 01:15 PM',
        priority: 'Medium',
        priorityClass: 'priority-medium',
        status: 'Pending',
        statusClass: 'status-pending'
      },
      {
        time: '16:00',
        title: 'Deep work block',
        detail: '04:00 PM – 05:30 PM',
        priority: 'Low',
        priorityClass: 'priority-low',
        status: 'Done',
        statusClass: 'status-done'
      }
    ].map((item) => `
      <div class="timeline-item">
        <span class="timeline-time">${item.time}</span>
        <div>
          <strong>${item.title}</strong>
          <small>${item.detail}</small>
          <div class="timeline-meta">
            <span class="priority-pill ${item.priorityClass}">${item.priority}</span>
            <span class="status-pill ${item.statusClass}">${item.status}</span>
          </div>
        </div>
      </div>
    `).join('');

    upcomingSchedules.innerHTML = [
      {
        title: 'Sprint retrospective',
        detail: 'Thursday, 11:00 AM',
        priority: 'Medium',
        priorityClass: 'priority-medium',
        status: 'Pending',
        statusClass: 'status-pending'
      },
      {
        title: 'Team sync',
        detail: 'Friday, 02:30 PM',
        priority: 'High',
        priorityClass: 'priority-high',
        status: 'Scheduled',
        statusClass: 'status-pending'
      }
    ].map((item) => `
      <div class="stack-item">
        <div class="list-row">
          <div>
            <strong>${item.title}</strong>
            <small>${item.detail}</small>
          </div>
        </div>
        <div class="timeline-meta">
          <span class="priority-pill ${item.priorityClass}">${item.priority}</span>
          <span class="status-pill ${item.statusClass}">${item.status}</span>
        </div>
      </div>
    `).join('');

    reminderList.innerHTML = [
      {
        title: 'Submit design notes',
        detail: 'For team review • 11:15 AM',
        status: 'Upcoming',
        statusClass: 'status-pending'
      },
      {
        title: 'Follow up on client request',
        detail: 'Due at 04:30 PM',
        status: 'Scheduled',
        statusClass: 'status-pending'
      }
    ].map((item) => `
      <div class="reminder-row">
        <div class="meta">
          <strong>${item.title}</strong>
          <small>${item.detail}</small>
        </div>
        <span class="status-pill ${item.statusClass}">${item.status}</span>
      </div>
    `).join('');

    categoryList.innerHTML = [
      { label: 'Work', color: '#5b6cff' },
      { label: 'Planning', color: '#7c3aed' },
      { label: 'Personal', color: '#1fb98a' },
      { label: 'Health', color: '#f7b955' }
    ].map((category) => `
      <span class="category-item"><span class="category-dot" style="background:${category.color};"></span>${category.label}</span>
    `).join('');
  }, 650);

  document.querySelectorAll('[data-action="add-schedule"]').forEach((button) => {
    button.addEventListener('click', () => {
      window.alert('Schedule creation UI will be added in a later stage.');
    });
  });

  document.querySelectorAll('[data-action="add-reminder"]').forEach((button) => {
    button.addEventListener('click', () => {
      window.alert('Reminder creation UI will be added in a later stage.');
    });
  });

  const logoutButton = document.querySelector('[data-action="logout"]');
  if (logoutButton) {
    logoutButton.addEventListener('click', () => {
      window.SCHEL.logout();
    });
  }
});
