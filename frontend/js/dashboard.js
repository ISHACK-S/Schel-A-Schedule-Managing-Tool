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

  const formatDate = (value) => {
    if (!value) return '—';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '—';
    return new Intl.DateTimeFormat('en', {
      month: 'short',
      day: 'numeric',
      year: 'numeric'
    }).format(date);
  };

  const formatTime = (value) => {
    if (!value) return '—';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '—';
    return new Intl.DateTimeFormat('en', {
      hour: 'numeric',
      minute: '2-digit'
    }).format(date);
  };

  const formatDateTime = (value) => {
    if (!value) return '—';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '—';
    return new Intl.DateTimeFormat('en', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: 'numeric',
      minute: '2-digit'
    }).format(date);
  };

  const priorityClass = (priority) => {
    const normalized = (priority || '').toString().trim().toUpperCase();
    if (normalized === 'HIGH') return 'priority-high';
    if (normalized === 'MEDIUM') return 'priority-medium';
    return 'priority-low';
  };

  const statusClass = (status) => {
    const normalized = (status || '').toString().trim().toUpperCase();
    if (normalized === 'COMPLETED') return 'status-done';
    if (normalized === 'MISSED') return 'status-missed';
    return 'status-pending';
  };

  const getScheduleDateValue = (schedule) => {
    if (!schedule) return null;
    if (schedule.task_date && schedule.start_time) {
      return new Date(`${schedule.task_date}T${schedule.start_time}`).getTime();
    }
    if (schedule.task_date) {
      return new Date(`${schedule.task_date}T00:00:00`).getTime();
    }
    return null;
  };

  const getReminderDateValue = (reminder) => {
    if (!reminder || !reminder.reminder_time) return null;
    return new Date(reminder.reminder_time).getTime();
  };

  const renderStats = (schedules, categories, reminders) => {
    const stats = {
      'total-schedules': schedules.length,
      pending: schedules.filter((schedule) => (schedule.status || '').toUpperCase() === 'PENDING').length,
      completed: schedules.filter((schedule) => (schedule.status || '').toUpperCase() === 'COMPLETED').length,
      missed: schedules.filter((schedule) => (schedule.status || '').toUpperCase() === 'MISSED').length,
      categories: categories.length,
      reminders: reminders.length
    };

    Object.entries(stats).forEach(([key, value]) => {
      const node = document.querySelector(`[data-stat="${key}"]`);
      if (node) {
        node.textContent = value;
      }
    });
  };

  const renderTodaySchedules = (schedules) => {
    const today = new Date();
    const todayIso = today.toISOString().slice(0, 10);
    const todays = schedules.filter((schedule) => (schedule.task_date || '').slice(0, 10) === todayIso)
      .sort((a, b) => (getScheduleDateValue(a) || 0) - (getScheduleDateValue(b) || 0));

    if (!todays.length) {
      todaySchedule.innerHTML = emptyState('No schedules today', 'You are all clear for today.');
      return;
    }

    todaySchedule.innerHTML = todays.slice(0, 3).map((schedule) => {
      const start = schedule.start_time ? formatTime(`${schedule.task_date}T${schedule.start_time}`) : 'Time TBD';
      const end = schedule.end_time ? formatTime(`${schedule.task_date}T${schedule.end_time}`) : '';
      const detail = end ? `${start} – ${end}` : start;
      return `
        <div class="timeline-item">
          <span class="timeline-time">${schedule.start_time ? schedule.start_time.slice(0, 5) : '—'}</span>
          <div>
            <strong>${schedule.title || 'Untitled schedule'}</strong>
            <small>${detail}</small>
            <div class="timeline-meta">
              <span class="priority-pill ${priorityClass(schedule.priority)}">${(schedule.priority || 'MEDIUM').toUpperCase()}</span>
              <span class="status-pill ${statusClass(schedule.status)}">${(schedule.status || 'PENDING').toUpperCase()}</span>
            </div>
          </div>
        </div>
      `;
    }).join('');
  };

  const renderUpcomingSchedules = (schedules) => {
    const upcoming = schedules
      .filter((schedule) => !['COMPLETED', 'MISSED'].includes((schedule.status || '').toUpperCase()))
      .filter((schedule) => {
        const value = getScheduleDateValue(schedule);
        if (value == null) return false;
        return value >= new Date().setHours(0, 0, 0, 0);
      })
      .sort((a, b) => (getScheduleDateValue(a) || 0) - (getScheduleDateValue(b) || 0))
      .slice(0, 4);

    if (!upcoming.length) {
      upcomingSchedules.innerHTML = emptyState('No upcoming schedules', 'Your next plan will appear here once it is created.');
      return;
    }

    upcomingSchedules.innerHTML = upcoming.map((schedule) => `
      <div class="stack-item">
        <div class="list-row">
          <div>
            <strong>${schedule.title || 'Untitled schedule'}</strong>
            <small>${formatDate(schedule.task_date)}${schedule.start_time ? ` · ${schedule.start_time.slice(0, 5)}` : ''}</small>
          </div>
        </div>
        <div class="timeline-meta">
          <span class="priority-pill ${priorityClass(schedule.priority)}">${(schedule.priority || 'MEDIUM').toUpperCase()}</span>
          <span class="status-pill ${statusClass(schedule.status)}">${(schedule.status || 'PENDING').toUpperCase()}</span>
        </div>
      </div>
    `).join('');
  };

  const renderReminderList = (reminders) => {
    const upcomingReminders = [...reminders]
      .filter((reminder) => reminder && reminder.reminder_time)
      .sort((a, b) => (getReminderDateValue(a) || 0) - (getReminderDateValue(b) || 0))
      .slice(0, 4);

    if (!upcomingReminders.length) {
      reminderList.innerHTML = emptyState('No reminders yet', 'Your active reminders will show here.');
      return;
    }

    reminderList.innerHTML = upcomingReminders.map((reminder) => `
      <div class="reminder-row">
        <div class="meta">
          <strong>${reminder.schedule_id ? `Reminder for ${reminder.schedule_id.slice(0, 8)}` : 'Reminder'}</strong>
          <small>${formatDateTime(reminder.reminder_time)}</small>
        </div>
        <span class="status-pill ${statusClass(reminder.is_sent ? 'COMPLETED' : 'PENDING')}">${reminder.is_sent ? 'Sent' : 'Upcoming'}</span>
      </div>
    `).join('');
  };

  const renderCategories = (categories) => {
    if (!categories.length) {
      categoryList.innerHTML = emptyState('No categories yet', 'Create categories to organize your work.');
      return;
    }

    categoryList.innerHTML = categories.slice(0, 8).map((category) => `
      <span class="category-item"><span class="category-dot" style="background:${category.color || '#5b6cff'};"></span>${category.name || 'Untitled category'}</span>
    `).join('');
  };

  const loadDashboardData = async () => {
    setLoadingState(todaySchedule, 3, 'schedule');
    setLoadingState(upcomingSchedules, 2);
    setLoadingState(reminderList, 2);
    setLoadingState(categoryList, 1);

    try {
      const [schedules, categories, reminders] = await Promise.all([
        window.SCHEL.scheduleApi.getSchedules(),
        window.SCHEL.categoryApi.getCategories(),
        window.SCHEL.reminderApi.getReminders()
      ]);

      renderStats(schedules, categories, reminders);
      renderTodaySchedules(schedules);
      renderUpcomingSchedules(schedules);
      renderReminderList(reminders);
      renderCategories(categories);
    } catch (error) {
      todaySchedule.innerHTML = emptyState('Unable to load dashboard data', 'Please try again in a moment.');
      upcomingSchedules.innerHTML = emptyState('No upcoming schedules', 'Schedule data could not be loaded.');
      reminderList.innerHTML = emptyState('No reminders yet', 'Reminder data could not be loaded.');
      categoryList.innerHTML = emptyState('No categories yet', 'Category data could not be loaded.');
      const message = error && error.message ? error.message : 'Unable to load dashboard data.';
      if (window.SCHEL && window.SCHEL.showToast) {
        window.SCHEL.showToast(message, 'error');
      }
    }
  };

  document.querySelectorAll('[data-action="add-schedule"]').forEach((button) => {
    button.addEventListener('click', () => {
      window.location.href = 'schedules.html';
    });
  });

  document.querySelectorAll('[data-action="add-reminder"]').forEach((button) => {
    button.addEventListener('click', () => {
      window.location.href = 'reminders.html';
    });
  });

  const logoutButton = document.querySelector('[data-action="logout"]');
  if (logoutButton) {
    logoutButton.addEventListener('click', () => {
      window.SCHEL.logout();
    });
  }

  loadDashboardData();
});
