document.addEventListener('DOMContentLoaded', () => {
  const dashboardUserName = document.getElementById('dashboard-user-name');
  const welcomeTag = document.getElementById('welcome-tag');
  const todaySchedule = document.getElementById('today-schedule');
  const upcomingSchedules = document.getElementById('upcoming-schedules');
  const reminderList = document.getElementById('reminder-list');
  const categoryList = document.getElementById('category-list');
  const scheduleStatusBadge = document.getElementById('dashboard-schedule-status');

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

  const emptyState = (title, message) => {
    const wrapper = document.createElement('div');
    wrapper.className = 'empty-state';
    const heading = document.createElement('strong');
    heading.textContent = title;
    const description = document.createElement('p');
    description.textContent = message;
    wrapper.append(heading, description);
    return wrapper;
  };

  const escapeHtml = (value) => String(value ?? '').replace(/[&<>"']/g, (character) => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;'
  })[character]);

  const localDateKey = (date) => [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, '0'),
    String(date.getDate()).padStart(2, '0')
  ].join('-');

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
    if (schedule.task_date) {
      const startTime = schedule.start_time || '00:00:00';
      const date = new Date(`${schedule.task_date}T${startTime}`);
      return Number.isNaN(date.getTime()) ? null : date.getTime();
    }
    return null;
  };

  const getReminderDateValue = (reminder) => {
    if (!reminder || !reminder.reminder_time) return null;
    return new Date(reminder.reminder_time).getTime();
  };

  const renderStats = (schedules, categories, reminders) => {
    const setStat = (key, value) => {
      const node = document.querySelector(`[data-stat="${key}"]`);
      if (node) node.textContent = value;
    };

    if (schedules) {
      setStat('total-schedules', schedules.length);
      setStat('pending', schedules.filter((schedule) => (schedule.status || '').toUpperCase() === 'PENDING').length);
      setStat('completed', schedules.filter((schedule) => (schedule.status || '').toUpperCase() === 'COMPLETED').length);
      setStat('missed', schedules.filter((schedule) => (schedule.status || '').toUpperCase() === 'MISSED').length);
      if (scheduleStatusBadge) {
        const hasMissed = schedules.some((schedule) => (schedule.status || '').toUpperCase() === 'MISSED');
        scheduleStatusBadge.textContent = schedules.length === 0 ? 'No schedules' : hasMissed ? 'Needs attention' : 'On track';
        scheduleStatusBadge.className = `badge ${hasMissed ? 'badge-warning' : schedules.length ? 'badge-success' : 'badge-neutral'}`;
      }
    }
    if (categories) setStat('categories', categories.length);
    if (reminders) setStat('reminders', reminders.length);
  };

  const renderTodaySchedules = (schedules) => {
    const todayIso = localDateKey(new Date());
    const todays = schedules.filter((schedule) => (schedule.task_date || '').slice(0, 10) === todayIso)
      .sort((a, b) => (getScheduleDateValue(a) || 0) - (getScheduleDateValue(b) || 0));

    if (!todays.length) {
      todaySchedule.replaceChildren(emptyState('No schedules today', 'You are all clear for today.'));
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
            <strong>${escapeHtml(schedule.title || 'Untitled schedule')}</strong>
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
        return value >= Date.now();
      })
      .sort((a, b) => (getScheduleDateValue(a) || 0) - (getScheduleDateValue(b) || 0))
      .slice(0, 4);

    if (!upcoming.length) {
      upcomingSchedules.replaceChildren(emptyState('No upcoming schedules', 'Your next plan will appear here once it is created.'));
      return;
    }

    upcomingSchedules.innerHTML = upcoming.map((schedule) => `
      <div class="stack-item">
        <div class="list-row">
          <div>
            <strong>${escapeHtml(schedule.title || 'Untitled schedule')}</strong>
              <small>${formatDate(schedule.task_date)}${schedule.start_time ? ` · ${escapeHtml(schedule.start_time.slice(0, 5))}` : ''}</small>
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
    const now = Date.now();
    const upcomingReminders = [...reminders]
      .filter((reminder) => reminder && reminder.reminder_time)
      .filter((reminder) => !reminder.is_sent && getReminderDateValue(reminder) >= now)
      .sort((a, b) => (getReminderDateValue(a) || 0) - (getReminderDateValue(b) || 0))
      .slice(0, 4);

    if (!upcomingReminders.length) {
      reminderList.replaceChildren(emptyState(
        reminders.length ? 'No upcoming reminders' : 'No reminders yet',
        reminders.length ? 'There are no upcoming unsent reminders.' : 'Your active reminders will show here.'
      ));
      return;
    }

    reminderList.innerHTML = upcomingReminders.map((reminder) => `
      <div class="reminder-row">
        <div class="meta">
            <strong>${escapeHtml(reminder.schedule_id ? `Reminder for ${getScheduleById(reminder.schedule_id)?.title || reminder.schedule_id.slice(0, 8)}` : 'Reminder')}</strong>
          <small>${formatDateTime(reminder.reminder_time)}</small>
        </div>
        <span class="status-pill ${statusClass(reminder.is_sent ? 'COMPLETED' : 'PENDING')}">${reminder.is_sent ? 'Sent' : 'Upcoming'}</span>
      </div>
    `).join('');
  };

  const renderCategories = (categories) => {
    if (!categories.length) {
      categoryList.replaceChildren(emptyState('No categories yet', 'Create categories to organize your work.'));
      return;
    }

    categoryList.innerHTML = categories.slice(0, 8).map((category) => `
      <span class="category-item"><span class="category-dot" style="background:${/^#[0-9a-fA-F]{3}([0-9a-fA-F]{3})?$/.test(category.color || '') ? category.color : '#5b6cff'};"></span>${escapeHtml(category.name || 'Untitled category')}</span>
    `).join('');
  };

  const loadDashboardData = async () => {
    setLoadingState(todaySchedule, 3, 'schedule');
    setLoadingState(upcomingSchedules, 2);
    setLoadingState(reminderList, 2);
    setLoadingState(categoryList, 1);

    const responses = await Promise.allSettled([
      window.SCHEL.api.getSchedules(),
      window.SCHEL.api.getCategories(),
      window.SCHEL.api.getReminders()
    ]);
    const [scheduleResponse, categoryResponse, reminderResponse] = responses;
    const schedules = scheduleResponse.status === 'fulfilled' && Array.isArray(scheduleResponse.value?.schedules)
      ? scheduleResponse.value.schedules
      : null;
    const categories = categoryResponse.status === 'fulfilled' && Array.isArray(categoryResponse.value?.categories)
      ? categoryResponse.value.categories
      : null;
    const reminders = reminderResponse.status === 'fulfilled' && Array.isArray(reminderResponse.value?.reminders)
      ? reminderResponse.value.reminders
      : null;

    renderStats(schedules, categories, reminders);
    if (schedules) {
      renderTodaySchedules(schedules);
      renderUpcomingSchedules(schedules);
    } else {
      const message = scheduleResponse.status === 'rejected'
        ? scheduleResponse.reason?.message || 'Unable to load schedules.'
        : 'The server returned an invalid schedules response.';
      if (scheduleStatusBadge) {
        scheduleStatusBadge.textContent = 'Unavailable';
        scheduleStatusBadge.className = 'badge badge-neutral';
      }
      todaySchedule.replaceChildren(emptyState('Unable to load today’s schedule', message));
      upcomingSchedules.replaceChildren(emptyState('Unable to load upcoming schedules', message));
    }
    if (reminders) {
      renderReminderList(reminders);
    } else {
      const message = reminderResponse.status === 'rejected'
        ? reminderResponse.reason?.message || 'Unable to load reminders.'
        : 'The server returned an invalid reminders response.';
      reminderList.replaceChildren(emptyState('Unable to load reminders', message));
    }
    if (categories) {
      renderCategories(categories);
    } else {
      const message = categoryResponse.status === 'rejected'
        ? categoryResponse.reason?.message || 'Unable to load categories.'
        : 'The server returned an invalid categories response.';
      categoryList.replaceChildren(emptyState('Unable to load categories', message));
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
