document.addEventListener('DOMContentLoaded', () => {
  if (!window.SCHEL.requireAuth()) {
    return;
  }

  const reminderList = document.getElementById('reminder-list');
  const reminderSearch = document.getElementById('reminder-search');
  const reminderStatusFilter = document.getElementById('reminder-status-filter');
  const reminderDateFilter = document.getElementById('reminder-date-filter');
  const reminderScheduleFilter = document.getElementById('reminder-schedule-filter');
  const reminderCountBadge = document.getElementById('reminder-count-badge');
  const reminderModal = document.getElementById('reminder-modal');
  const reminderDetailModal = document.getElementById('reminder-detail-modal');
  const deleteReminderModal = document.getElementById('delete-reminder-modal');
  const reminderForm = document.getElementById('reminder-form');
  const reminderDetailBody = document.getElementById('reminder-detail-body');
  const reminderScheduleSelect = document.getElementById('reminder-schedule');
  const toastContainer = document.getElementById('toast-container');
  const sortToggle = document.getElementById('sort-toggle');

  const state = {
    reminders: [],
    schedules: [],
    pendingEditId: null,
    pendingDeleteId: null,
    sortMode: 'soonest'
  };

  const showToast = (message, type = 'success') => {
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = message;
    toastContainer.appendChild(toast);

    setTimeout(() => toast.classList.add('is-visible'), 20);
    setTimeout(() => {
      toast.classList.remove('is-visible');
      setTimeout(() => toast.remove(), 220);
    }, 2600);
  };

  const clearFormErrors = () => {
    document.querySelectorAll('.field-error').forEach((node) => {
      node.textContent = '';
    });
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

  const formatDateOnly = (value) => {
    if (!value) return '—';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '—';
    return new Intl.DateTimeFormat('en', {
      month: 'short',
      day: 'numeric',
      year: 'numeric'
    }).format(date);
  };

  const formatTimeOnly = (value) => {
    if (!value) return '—';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '—';
    return new Intl.DateTimeFormat('en', {
      hour: 'numeric',
      minute: '2-digit'
    }).format(date);
  };

  const getScheduleById = (scheduleId) => state.schedules.find((schedule) => String(schedule.id) === String(scheduleId));

  const getPersistedReminder = async (id, scheduleId, reminderTime) => {
    const result = await window.SCHEL.api.getReminders();
    if (!Array.isArray(result?.reminders)) {
      throw new Error('The server returned an invalid reminders response.');
    }
    if (id) {
      return result.reminders.find((reminder) => String(reminder.id) === String(id)) || null;
    }
    const targetTime = new Date(reminderTime).getTime();
    return result.reminders.find((reminder) => (
      String(reminder.schedule_id) === String(scheduleId)
      && new Date(reminder.reminder_time).getTime() === targetTime
    )) || null;
  };

  const getScheduleDisplay = (schedule) => {
    if (!schedule) return 'Unassigned schedule';
    const parts = [schedule.title || 'Untitled schedule'];
    if (schedule.task_date) parts.push(formatDateOnly(schedule.task_date));
    if (schedule.start_time) parts.push(formatTimeOnly(schedule.start_time));
    return parts.join(' · ');
  };

  const buildEmptyState = (title, message, actionText, actionHandler) => {
    const wrapper = document.createElement('div');
    wrapper.className = 'empty-state';
    const icon = document.createElement('div');
    icon.className = 'empty-icon';
    icon.textContent = '🔔';
    const heading = document.createElement('h3');
    heading.textContent = title;
    const description = document.createElement('p');
    description.textContent = message;
    const button = document.createElement('button');
    button.className = 'btn btn-primary';
    button.type = 'button';
    button.textContent = actionText;
    button.addEventListener('click', actionHandler);
    wrapper.append(icon, heading, description, button);
    return wrapper;
  };

  const renderLoading = () => {
    reminderList.innerHTML = '';
    const skeletons = Array.from({ length: 3 }, () => {
      const card = document.createElement('div');
      card.className = 'reminder-card loading-card';
      card.innerHTML = `
        <div class="load-skeleton" style="height: 16px; width: 36%; margin-bottom: 12px;"></div>
        <div class="load-skeleton" style="height: 18px; width: 62%; margin-bottom: 14px;"></div>
        <div class="load-skeleton" style="height: 12px; width: 72%; margin-bottom: 20px;"></div>
        <div class="load-skeleton" style="height: 32px; width: 120px; border-radius: 999px;"></div>
      `;
      return card;
    });
    skeletons.forEach((card) => reminderList.appendChild(card));
  };

  const updateSortButton = () => {
    sortToggle.textContent = state.sortMode === 'soonest' ? 'Sort: Soonest' : 'Sort: Latest';
  };

  const populateScheduleOptions = () => {
    reminderScheduleFilter.replaceChildren(new Option('All schedules', 'ALL'));
    reminderScheduleSelect.replaceChildren(new Option('Select a schedule', ''));

    state.schedules.forEach((schedule) => {
      const label = `${schedule.title || 'Untitled schedule'} · ${schedule.task_date || 'No date'}`;
      reminderScheduleFilter.add(new Option(label, schedule.id));
      reminderScheduleSelect.add(new Option(label, schedule.id));
    });

    reminderScheduleSelect.disabled = state.schedules.length === 0;
  };

  const getVisibleReminders = () => {
    const searchTerm = (reminderSearch.value || '').trim().toLowerCase();
    const statusFilter = reminderStatusFilter.value;
    const dateFilter = reminderDateFilter.value;
    const scheduleFilter = reminderScheduleFilter.value;

    let visible = [...state.reminders];

    if (searchTerm) {
      visible = visible.filter((reminder) => {
        const schedule = getScheduleById(reminder.schedule_id);
        const lookup = `${schedule ? schedule.title : ''} ${formatDateTime(reminder.reminder_time)}`.toLowerCase();
        return lookup.includes(searchTerm);
      });
    }

    if (statusFilter !== 'ALL') {
      visible = visible.filter((reminder) => {
        const isPending = !reminder.is_sent;
        return statusFilter === 'PENDING' ? isPending : !isPending;
      });
    }

    if (dateFilter) {
      visible = visible.filter((reminder) => {
        const reminderDate = new Date(reminder.reminder_time);
        if (Number.isNaN(reminderDate.getTime())) return false;
        return reminderDate.toISOString().slice(0, 10) === dateFilter;
      });
    }

    if (scheduleFilter !== 'ALL') {
      visible = visible.filter((reminder) => String(reminder.schedule_id) === String(scheduleFilter));
    }

    visible.sort((a, b) => {
      const diff = new Date(a.reminder_time) - new Date(b.reminder_time);
      return state.sortMode === 'soonest' ? diff : -diff;
    });

    return visible;
  };

  const renderReminderList = () => {
    const visible = getVisibleReminders();
    reminderCountBadge.textContent = `${visible.length} ${visible.length === 1 ? 'reminder' : 'reminders'}`;
    reminderList.innerHTML = '';

    if (state.reminders.length === 0) {
      reminderList.appendChild(buildEmptyState(
        'No reminders yet',
        'Stay on top of your schedule by creating your first reminder.',
        '+ Add Reminder',
        openCreateModal
      ));
      return;
    }

    if (visible.length === 0) {
      reminderList.appendChild(buildEmptyState(
        'No matching reminders',
        'Try adjusting your search or filters.',
        'Clear Filters',
        () => {
          reminderSearch.value = '';
          reminderStatusFilter.value = 'ALL';
          reminderDateFilter.value = '';
          reminderScheduleFilter.value = 'ALL';
          renderReminderList();
        }
      ));
      return;
    }

    visible.forEach((reminder) => {
      const schedule = getScheduleById(reminder.schedule_id);
      const card = document.createElement('article');
      card.className = 'reminder-card';
      card.innerHTML = `
        <div class="reminder-card-header">
          <div>
            <p class="eyebrow reminder-label">Reminder</p>
            <h3>${schedule ? schedule.title : 'Schedule reminder'}</h3>
          </div>
          <span class="badge ${reminder.is_sent ? 'badge-success' : 'badge-warning'}">${reminder.is_sent ? 'Sent' : 'Pending'}</span>
        </div>

        <div class="reminder-card-meta">
          <div>
            <span class="detail-label">Time</span>
            <strong>${formatDateTime(reminder.reminder_time)}</strong>
          </div>
          <div>
            <span class="detail-label">Schedule</span>
            <strong>${getScheduleDisplay(schedule)}</strong>
          </div>
        </div>

        <div class="reminder-card-actions">
          <button class="btn btn-secondary btn-small" type="button" data-view-id="${reminder.id}">View</button>
          <button class="btn btn-secondary btn-small" type="button" data-edit-id="${reminder.id}">Edit</button>
          <button class="btn btn-danger btn-small" type="button" data-delete-id="${reminder.id}">Delete</button>
        </div>
      `;

      card.querySelector('[data-view-id]').addEventListener('click', () => openDetailModal(reminder.id));
      card.querySelector('[data-edit-id]').addEventListener('click', () => openEditModal(reminder.id));
      card.querySelector('[data-delete-id]').addEventListener('click', () => openDeleteModal(reminder.id));
      reminderList.appendChild(card);
    });
  };

  const openCreateModal = () => {
    clearFormErrors();
    reminderForm.reset();
    state.pendingEditId = null;
    document.getElementById('reminder-modal-title').textContent = 'Create Reminder';
    document.getElementById('save-reminder-btn').textContent = 'Create Reminder';
    reminderScheduleSelect.value = state.schedules.length ? state.schedules[0].id : '';
    reminderScheduleSelect.disabled = state.schedules.length === 0;
    if (state.schedules.length === 0) {
      showToast('Create a schedule before adding a reminder.', 'error');
    }
    reminderModal.classList.add('open');
    reminderModal.setAttribute('aria-hidden', 'false');
  };

  const openEditModal = (id) => {
    const reminder = state.reminders.find((item) => item.id === id);
    if (!reminder) return;
    clearFormErrors();
    state.pendingEditId = id;
    document.getElementById('reminder-modal-title').textContent = 'Edit Reminder';
    document.getElementById('save-reminder-btn').textContent = 'Update Reminder';
    reminderScheduleSelect.value = reminder.schedule_id || '';
    reminderScheduleSelect.disabled = true;
    if (reminder.reminder_time) {
      const date = new Date(reminder.reminder_time);
      const localValue = new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
      document.getElementById('reminder-time').value = localValue;
    }
    reminderModal.classList.add('open');
    reminderModal.setAttribute('aria-hidden', 'false');
  };

  const closeModal = (modal) => {
    modal.classList.remove('open');
    modal.setAttribute('aria-hidden', 'true');
  };

  const closeAllModals = () => {
    closeModal(reminderModal);
    closeModal(reminderDetailModal);
    closeModal(deleteReminderModal);
  };

  const openDetailModal = (id) => {
    const reminder = state.reminders.find((item) => item.id === id);
    if (!reminder) return;
    const schedule = getScheduleById(reminder.schedule_id);

    reminderDetailBody.innerHTML = `
      <div class="detail-grid">
        <div>
          <span class="detail-label">Reminder</span>
          <strong>${schedule ? schedule.title : 'Schedule reminder'}</strong>
        </div>
        <div>
          <span class="detail-label">Status</span>
          <strong><span class="badge ${reminder.is_sent ? 'badge-success' : 'badge-warning'}">${reminder.is_sent ? 'Sent' : 'Pending'}</span></strong>
        </div>
        <div class="detail-full">
          <span class="detail-label">Scheduled for</span>
          <strong>${formatDateTime(reminder.reminder_time)}</strong>
        </div>
        <div class="detail-full">
          <span class="detail-label">Associated schedule</span>
          <p>${getScheduleDisplay(schedule)}</p>
        </div>
      </div>
    `;

    reminderDetailModal.classList.add('open');
    reminderDetailModal.setAttribute('aria-hidden', 'false');
  };

  const openDeleteModal = (id) => {
    const reminder = state.reminders.find((item) => item.id === id);
    if (!reminder) return;
    state.pendingDeleteId = id;
    const schedule = getScheduleById(reminder.schedule_id);
    const label = schedule ? schedule.title : 'this reminder';
    document.getElementById('delete-reminder-message').textContent = `Are you sure you want to delete "${label}"?`;
    deleteReminderModal.classList.add('open');
    deleteReminderModal.setAttribute('aria-hidden', 'false');
  };

  const validateReminderPayload = (payload) => {
    const errors = {};
    if (!payload.schedule_id) {
      errors.schedule_id = 'Please select a schedule.';
    }

    if (!payload.reminder_time) {
      errors.reminder_time = 'Please select a reminder date and time.';
    } else if (Number.isNaN(new Date(payload.reminder_time).getTime())) {
      errors.reminder_time = 'Please enter a valid reminder date and time.';
    }

    return errors;
  };

  const applyFormErrors = (errors) => {
    clearFormErrors();
    Object.entries(errors).forEach(([key, value]) => {
      const field = document.querySelector(`[data-error-for="${key}"]`);
      if (field) field.textContent = value;
    });
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    const payload = {
      schedule_id: reminderScheduleSelect.value,
      reminder_time: document.getElementById('reminder-time').value
    };

    const errors = validateReminderPayload(payload);
    if (Object.keys(errors).length) {
      applyFormErrors(errors);
      return;
    }

    const saveButton = document.getElementById('save-reminder-btn');
    if (saveButton.disabled) return;
    const originalButtonText = saveButton.textContent;
    const reminderTimeUtc = new Date(payload.reminder_time).toISOString();
    saveButton.disabled = true;
    saveButton.classList.add('is-loading');
    saveButton.textContent = state.pendingEditId ? 'Updating…' : 'Creating…';

    try {
      let result;
      if (state.pendingEditId) {
        try {
          result = await window.SCHEL.api.updateReminder(state.pendingEditId, {
            reminder_time: reminderTimeUtc
          });
        } catch (requestError) {
          const persisted = await getPersistedReminder(state.pendingEditId);
          if (!persisted || new Date(persisted.reminder_time).getTime() !== new Date(reminderTimeUtc).getTime()) {
            throw requestError;
          }
          result = { reminder: persisted };
        }
        const updatedReminder = result?.reminder?.id
          ? result.reminder
          : await getPersistedReminder(state.pendingEditId);
        if (!updatedReminder?.id) throw new Error('The server did not return the updated reminder.');
        const index = state.reminders.findIndex((item) => item.id === updatedReminder.id);
        if (index >= 0) state.reminders[index] = updatedReminder;
        showToast('Reminder updated successfully.', 'success');
      } else {
        try {
          result = await window.SCHEL.api.createReminder({
            schedule_id: payload.schedule_id,
            reminder_time: reminderTimeUtc
          });
        } catch (requestError) {
          const persisted = await getPersistedReminder(null, payload.schedule_id, reminderTimeUtc);
          if (!persisted) throw requestError;
          result = { reminder: persisted };
        }
        const createdReminder = result?.reminder?.id
          ? result.reminder
          : await getPersistedReminder(null, payload.schedule_id, reminderTimeUtc);
        if (!createdReminder?.id) throw new Error('The server did not return the created reminder.');
        state.reminders.push(createdReminder);
        showToast('Reminder created successfully.', 'success');
      }

      reminderForm.reset();
      state.pendingEditId = null;
      clearFormErrors();
      closeModal(reminderModal);
      renderReminderList();
    } catch (error) {
      showToast(error?.message || 'Could not save this reminder.', 'error');
    } finally {
      saveButton.disabled = false;
      saveButton.classList.remove('is-loading');
      saveButton.textContent = originalButtonText;
      reminderScheduleSelect.disabled = state.schedules.length === 0 || Boolean(state.pendingEditId);
    }
  };

  const handleDelete = async () => {
    if (!state.pendingDeleteId) return;
    const deleteButton = document.getElementById('confirm-delete-reminder-btn');
    if (deleteButton.disabled) return;
    const originalButtonText = deleteButton.textContent;
    deleteButton.disabled = true;
    deleteButton.classList.add('is-loading');
    deleteButton.textContent = 'Deleting…';

    try {
      await window.SCHEL.api.deleteReminder(state.pendingDeleteId);
      showToast('Reminder deleted successfully.', 'success');
      closeModal(deleteReminderModal);
      state.pendingDeleteId = null;
      await refreshReminders();
    } catch (error) {
      showToast(error?.message || 'Could not delete this reminder.', 'error');
    } finally {
      deleteButton.disabled = false;
      deleteButton.classList.remove('is-loading');
      deleteButton.textContent = originalButtonText;
    }
  };

  const refreshSchedules = async () => {
    try {
      state.schedules = await window.SCHEL.scheduleApi.getSchedules();
      populateScheduleOptions();
    } catch (error) {
      state.schedules = [];
      populateScheduleOptions();
      showToast(error?.message || 'Unable to load schedules for reminders.', 'error');
    }
  };

  const refreshReminders = async () => {
    renderLoading();
    try {
      const result = await window.SCHEL.api.getReminders();
      if (!Array.isArray(result?.reminders)) {
        throw new Error('The server returned an invalid reminders response.');
      }
      state.reminders = result.reminders;
      renderReminderList();
    } catch (error) {
      reminderList.innerHTML = '';
      reminderList.appendChild(buildEmptyState('Unable to load reminders', error?.message || 'Please try again in a moment.', 'Try Again', refreshReminders));
      showToast(error?.message || 'Unable to load reminders.', 'error');
    }
  };

  reminderSearch.addEventListener('input', renderReminderList);
  reminderStatusFilter.addEventListener('change', renderReminderList);
  reminderDateFilter.addEventListener('change', renderReminderList);
  reminderScheduleFilter.addEventListener('change', renderReminderList);
  reminderForm.addEventListener('submit', handleSubmit);
  document.getElementById('open-create-modal').addEventListener('click', openCreateModal);
  document.getElementById('confirm-delete-reminder-btn').addEventListener('click', handleDelete);
  document.getElementById('clear-filters').addEventListener('click', () => {
    reminderSearch.value = '';
    reminderStatusFilter.value = 'ALL';
    reminderDateFilter.value = '';
    reminderScheduleFilter.value = 'ALL';
    renderReminderList();
  });

  sortToggle.addEventListener('click', () => {
    state.sortMode = state.sortMode === 'soonest' ? 'latest' : 'soonest';
    updateSortButton();
    renderReminderList();
  });

  document.querySelectorAll('[data-close-modal]').forEach((button) => {
    button.addEventListener('click', () => {
      closeAllModals();
    });
  });

  const logoutButton = document.querySelector('[data-action="logout"]');
  if (logoutButton) {
    logoutButton.addEventListener('click', () => {
      window.SCHEL.logout();
    });
  }

  updateSortButton();
  refreshSchedules();
  refreshReminders();
});
