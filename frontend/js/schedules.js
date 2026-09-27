document.addEventListener('DOMContentLoaded', () => {
  if (!window.SCHEL.requireAuth()) {
    return;
  }

  const user = window.SCHEL.getCurrentUser();
  const scheduleList = document.getElementById('schedule-list');
  const scheduleSearch = document.getElementById('schedule-search');
  const scheduleDateFilter = document.getElementById('schedule-date-filter');
  const scheduleStatusFilter = document.getElementById('schedule-status-filter');
  const schedulePriorityFilter = document.getElementById('schedule-priority-filter');
  const scheduleCategoryFilter = document.getElementById('schedule-category-filter');
  const sortToggle = document.getElementById('sort-toggle');
  const clearFiltersButton = document.getElementById('clear-filters');
  const openCreateModalButton = document.getElementById('open-create-modal');
  const scheduleModal = document.getElementById('schedule-modal');
  const detailModal = document.getElementById('schedule-detail-modal');
  const deleteModal = document.getElementById('delete-modal');
  const scheduleForm = document.getElementById('schedule-form');
  const toastContainer = document.getElementById('toast-container');
  const scheduleCountBadge = document.getElementById('schedule-count-badge');

  const state = {
    schedules: [],
    categories: [],
    sortMode: 'date-asc',
    pendingCreateId: null,
    pendingDeleteId: null,
    selectedDetailId: null,
  };

  const toTitleCase = (value) => (value || '').replace(/\b\w/g, (char) => char.toUpperCase());

  const showToast = (message, type = 'success') => {
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = message;
    toastContainer.appendChild(toast);

    setTimeout(() => {
      toast.classList.add('is-visible');
    }, 20);

    setTimeout(() => {
      toast.classList.remove('is-visible');
      setTimeout(() => toast.remove(), 220);
    }, 2600);
  };

  const formatDate = (value) => {
    if (!value) return 'No date';
    const parsed = new Date(`${value}T00:00:00`);
    if (Number.isNaN(parsed.getTime())) return value;
    return parsed.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
  };

  const formatTime = (value) => {
    if (!value) return '—';
    const [hours, minutes] = value.split(':').map(Number);
    if (Number.isNaN(hours) || Number.isNaN(minutes)) return value;
    const date = new Date();
    date.setHours(hours, minutes, 0, 0);
    return date.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' });
  };

  const getScheduleCategoryName = (schedule) => {
    const category = state.categories.find((item) => item.id === schedule.category_id);
    return category?.name || 'Unassigned';
  };

  const populateCategorySelectors = () => {
    const filterValue = scheduleCategoryFilter.value || 'ALL';
    scheduleCategoryFilter.replaceChildren(new Option('All', 'ALL'));
    state.categories.forEach((category) => {
      scheduleCategoryFilter.add(new Option(category.name, category.id));
    });
    scheduleCategoryFilter.value = state.categories.some((category) => category.id === filterValue)
      ? filterValue
      : 'ALL';

    const formSelector = document.getElementById('schedule-category');
    formSelector.replaceChildren(new Option('No category', ''));
    state.categories.forEach((category) => {
      formSelector.add(new Option(category.name, category.id));
    });
  };

  const setScheduleCount = (list) => {
    scheduleCountBadge.textContent = `${list.length} ${list.length === 1 ? 'schedule' : 'schedules'}`;
  };

  const getPriorityClass = (priority) => {
    const value = (priority || '').toUpperCase();
    if (value === 'LOW') return 'priority-low';
    if (value === 'HIGH') return 'priority-high';
    return 'priority-medium';
  };

  const getStatusClass = (status) => {
    const value = (status || '').toUpperCase();
    if (value === 'COMPLETED') return 'status-completed';
    if (value === 'MISSED') return 'status-missed';
    return 'status-pending';
  };

  const clearFormErrors = () => {
    document.querySelectorAll('.field-error').forEach((error) => {
      error.textContent = '';
    });
  };

  const buildEmptyState = (title, message, actionText, actionHandler) => {
    const wrapper = document.createElement('div');
    wrapper.className = 'empty-state';
    wrapper.innerHTML = `
      <div class="empty-icon">📅</div>
      <h3>${title}</h3>
      <p>${message}</p>
      <button class="btn btn-primary" type="button">${actionText}</button>
    `;
    wrapper.querySelector('button').addEventListener('click', actionHandler);
    return wrapper;
  };

  const renderLoading = () => {
    scheduleList.innerHTML = '';
    const skeletons = Array.from({ length: 3 }, (_, index) => {
      const card = document.createElement('div');
      card.className = 'schedule-card loading-card';
      card.innerHTML = `
        <div class="load-skeleton" style="height: 18px; width: 50%; margin-bottom: 12px;"></div>
        <div class="load-skeleton" style="height: 12px; width: 82%; margin-bottom: 10px;"></div>
        <div class="load-skeleton" style="height: 12px; width: 60%; margin-bottom: 18px;"></div>
        <div class="load-skeleton" style="height: 32px; width: 120px; border-radius: 999px;"></div>
      `;
      return card;
    });
    skeletons.forEach((card) => scheduleList.appendChild(card));
  };

  const getVisibleSchedules = () => {
    const searchTerm = (scheduleSearch.value || '').trim().toLowerCase();
    const dateFilter = scheduleDateFilter.value;
    const status = scheduleStatusFilter.value;
    const priority = schedulePriorityFilter.value;
    const category = scheduleCategoryFilter.value;

    const filtered = state.schedules.filter((schedule) => {
      const matchesSearch = !searchTerm || `${schedule.title || ''} ${schedule.description || ''}`.toLowerCase().includes(searchTerm);
      const matchesDate = !dateFilter || (schedule.task_date || '').toString() === dateFilter;
      const matchesStatus = status === 'ALL' || (schedule.status || '').toUpperCase() === status;
      const matchesPriority = priority === 'ALL' || (schedule.priority || '').toUpperCase() === priority;
      const matchesCategory = category === 'ALL' || schedule.category_id === category;
      return matchesSearch && matchesDate && matchesStatus && matchesPriority && matchesCategory;
    });

    filtered.sort((left, right) => {
      const leftDate = left.task_date || '';
      const rightDate = right.task_date || '';
      const leftTime = left.start_time || '00:00';
      const rightTime = right.start_time || '00:00';

      if (state.sortMode === 'newest') {
        return new Date(rightDate + 'T' + rightTime) - new Date(leftDate + 'T' + leftTime);
      }
      if (state.sortMode === 'oldest') {
        return new Date(leftDate + 'T' + leftTime) - new Date(rightDate + 'T' + rightTime);
      }
      if (state.sortMode === 'priority') {
        const order = { HIGH: 3, MEDIUM: 2, LOW: 1 };
        return (order[(right.priority || 'MEDIUM').toUpperCase()] || 0) - (order[(left.priority || 'MEDIUM').toUpperCase()] || 0);
      }
      if (state.sortMode === 'date-desc') {
        return new Date(rightDate + 'T' + rightTime) - new Date(leftDate + 'T' + leftTime);
      }
      return new Date(leftDate + 'T' + leftTime) - new Date(rightDate + 'T' + rightTime);
    });

    return filtered;
  };

  const renderScheduleList = () => {
    const visible = getVisibleSchedules();
    setScheduleCount(visible);
    scheduleList.innerHTML = '';

    if (visible.length === 0) {
      const hasSchedules = state.schedules.length > 0;
      const title = hasSchedules ? 'No matching schedules' : 'No schedules yet';
      const message = hasSchedules ? 'Try adjusting your filters or search.' : 'Your calendar is clear. Create your first schedule to get started.';
      const actionText = hasSchedules ? 'Clear Filters' : '+ Add Schedule';
      const actionHandler = hasSchedules ? () => {
        scheduleSearch.value = '';
        scheduleDateFilter.value = '';
        scheduleStatusFilter.value = 'ALL';
        schedulePriorityFilter.value = 'ALL';
        scheduleCategoryFilter.value = 'ALL';
        renderScheduleList();
      } : () => openCreateModal();
      scheduleList.appendChild(buildEmptyState(title, message, actionText, actionHandler));
      return;
    }

    visible.forEach((schedule) => {
      const card = document.createElement('article');
      card.className = 'schedule-card';
      card.innerHTML = `
        <div class="schedule-card-header">
          <div>
            <h3>${schedule.title || 'Untitled schedule'}</h3>
            <p>${schedule.description || 'No description provided.'}</p>
          </div>
          <button class="icon-button" type="button" data-detail-id="${schedule.id}" aria-label="View details">↗</button>
        </div>
        <div class="schedule-meta">
          <span>${formatDate(schedule.task_date)}</span>
          <span>${formatTime(schedule.start_time)} – ${formatTime(schedule.end_time)}</span>
        </div>
        <div class="schedule-badges">
          <span class="badge badge-priority ${getPriorityClass(schedule.priority)}">${toTitleCase(schedule.priority || 'MEDIUM')}</span>
          <span class="badge badge-status ${getStatusClass(schedule.status)}">${toTitleCase(schedule.status || 'PENDING')}</span>
        </div>
        <div class="schedule-footer">
          <span class="category-pill">Category: ${getScheduleCategoryName(schedule)}</span>
          <div class="schedule-actions">
            <button class="btn btn-secondary btn-small" type="button" data-edit-id="${schedule.id}">Edit</button>
            <button class="btn btn-danger btn-small" type="button" data-delete-id="${schedule.id}">Delete</button>
          </div>
        </div>
      `;

      card.addEventListener('click', (event) => {
        const detailTarget = event.target.closest('[data-detail-id]');
        if (detailTarget) {
          openDetailModal(detailTarget.getAttribute('data-detail-id'));
          return;
        }

        const editTarget = event.target.closest('[data-edit-id]');
        if (editTarget) {
          event.stopPropagation();
          openEditModal(editTarget.getAttribute('data-edit-id'));
          return;
        }

        const deleteTarget = event.target.closest('[data-delete-id]');
        if (deleteTarget) {
          event.stopPropagation();
          openDeleteModal(deleteTarget.getAttribute('data-delete-id'));
          return;
        }

        const rowTarget = event.target.closest('.schedule-card');
        if (rowTarget && !event.target.closest('button')) {
          openDetailModal(schedule.id);
        }
      });

      scheduleList.appendChild(card);
    });
  };

  const openCreateModal = () => {
    clearFormErrors();
    scheduleForm.reset();
    document.getElementById('schedule-modal-title').textContent = 'Create Schedule';
    document.getElementById('save-schedule-btn').textContent = 'Save Schedule';
    state.pendingCreateId = null;
    document.getElementById('schedule-priority').value = 'MEDIUM';
    document.getElementById('schedule-status').value = 'PENDING';
    document.getElementById('schedule-category').value = state.categories[0]?.id || '';
    scheduleModal.classList.add('open');
    scheduleModal.setAttribute('aria-hidden', 'false');
  };

  const openEditModal = (id) => {
    const schedule = state.schedules.find((item) => item.id === id);
    if (!schedule) return;
    clearFormErrors();
    state.pendingCreateId = id;
    document.getElementById('schedule-modal-title').textContent = 'Edit Schedule';
    document.getElementById('save-schedule-btn').textContent = 'Update Schedule';
    document.getElementById('schedule-title').value = schedule.title || '';
    document.getElementById('schedule-description').value = schedule.description || '';
    document.getElementById('schedule-date').value = schedule.task_date || '';
    document.getElementById('schedule-start-time').value = schedule.start_time || '';
    document.getElementById('schedule-end-time').value = schedule.end_time || '';
    document.getElementById('schedule-priority').value = (schedule.priority || 'MEDIUM').toUpperCase();
    document.getElementById('schedule-status').value = (schedule.status || 'PENDING').toUpperCase();
    const categorySelector = document.getElementById('schedule-category');
    categorySelector.value = schedule.category_id || '';
    scheduleModal.classList.add('open');
    scheduleModal.setAttribute('aria-hidden', 'false');
  };

  const closeModal = (modal) => {
    modal.classList.remove('open');
    modal.setAttribute('aria-hidden', 'true');
  };

  const closeAllModals = () => {
    closeModal(scheduleModal);
    closeModal(detailModal);
    closeModal(deleteModal);
  };

  const openDetailModal = (id) => {
    const schedule = state.schedules.find((item) => item.id === id);
    if (!schedule) return;
    state.selectedDetailId = id;

    document.getElementById('schedule-detail-title').textContent = schedule.title || 'Schedule Details';
    document.getElementById('schedule-detail-body').innerHTML = `
      <div class="detail-grid">
        <div>
          <span class="detail-label">Title</span>
          <strong>${schedule.title || 'Untitled schedule'}</strong>
        </div>
        <div>
          <span class="detail-label">Category</span>
          <strong>${getScheduleCategoryName(schedule)}</strong>
        </div>
        <div>
          <span class="detail-label">Date</span>
          <strong>${formatDate(schedule.task_date)}</strong>
        </div>
        <div>
          <span class="detail-label">Time</span>
          <strong>${formatTime(schedule.start_time)} – ${formatTime(schedule.end_time)}</strong>
        </div>
        <div>
          <span class="detail-label">Priority</span>
          <strong><span class="badge badge-priority ${getPriorityClass(schedule.priority)}">${toTitleCase(schedule.priority || 'MEDIUM')}</span></strong>
        </div>
        <div>
          <span class="detail-label">Status</span>
          <strong><span class="badge badge-status ${getStatusClass(schedule.status)}">${toTitleCase(schedule.status || 'PENDING')}</span></strong>
        </div>
        <div class="detail-full">
          <span class="detail-label">Description</span>
          <p>${schedule.description ? schedule.description : 'No description provided.'}</p>
        </div>
      </div>
    `;

    const markCompleteButton = document.getElementById('detail-mark-complete');
    const shouldShowComplete = (schedule.status || '').toUpperCase() !== 'COMPLETED';
    markCompleteButton.style.display = shouldShowComplete ? 'inline-flex' : 'none';
    markCompleteButton.onclick = async () => {
      try {
        const updated = await window.SCHEL.scheduleApi.updateSchedule({ ...schedule, status: 'COMPLETED' });
        const index = state.schedules.findIndex((item) => item.id === updated.id);
        if (index >= 0) state.schedules[index] = updated;
        renderScheduleList();
        closeModal(detailModal);
        showToast('Schedule updated successfully.', 'success');
      } catch (error) {
        showToast('Could not update the schedule.', 'error');
      }
    };

    detailModal.classList.add('open');
    detailModal.setAttribute('aria-hidden', 'false');
  };

  const openDeleteModal = (id) => {
    const schedule = state.schedules.find((item) => item.id === id);
    if (!schedule) return;
    state.pendingDeleteId = id;
    document.getElementById('delete-message').textContent = `Are you sure you want to delete "${schedule.title || 'this schedule'}"?`;
    deleteModal.classList.add('open');
    deleteModal.setAttribute('aria-hidden', 'false');
  };

  const validateSchedulePayload = (payload) => {
    const errors = {};
    const title = (payload.title || '').trim();
    const date = payload.task_date;
    const startTime = payload.start_time;
    const endTime = payload.end_time;
    const priority = (payload.priority || '').toUpperCase();
    const status = (payload.status || '').toUpperCase();

    if (!title) errors.title = 'Title is required.';
    if (!date) errors.task_date = 'Date is required.';
    if (!startTime) errors.start_time = 'Start time is required.';
    if (!endTime) errors.end_time = 'End time is required.';
    if (startTime && endTime && startTime >= endTime) errors.end_time = 'End time must be later than start time.';
    if (priority && !['LOW', 'MEDIUM', 'HIGH'].includes(priority)) errors.priority = 'Priority must be LOW, MEDIUM, or HIGH.';
    if (status && !['PENDING', 'COMPLETED', 'MISSED'].includes(status)) errors.status = 'Status must be PENDING, COMPLETED, or MISSED.';

    return errors;
  };

  const applyFormErrors = (errors) => {
    clearFormErrors();
    Object.entries(errors).forEach(([key, value]) => {
      const field = document.querySelector(`[data-error-for="${key}"]`);
      if (field) field.textContent = value;
    });
  };

  const handleScheduleSubmit = async (event) => {
    event.preventDefault();
    const formData = new FormData(scheduleForm);
    const payload = {
      id: state.pendingCreateId || undefined,
      title: formData.get('title') || '',
      description: formData.get('description') || '',
      task_date: formData.get('task_date') || '',
      start_time: formData.get('start_time') || '',
      end_time: formData.get('end_time') || '',
      priority: formData.get('priority') || 'MEDIUM',
      status: formData.get('status') || 'PENDING',
      category_id: formData.get('category') || null
    };

    const errors = validateSchedulePayload(payload);
    if (Object.keys(errors).length) {
      applyFormErrors(errors);
      return;
    }

    try {
      let result;
      if (state.pendingCreateId) {
        result = await window.SCHEL.scheduleApi.updateSchedule(payload);
        const index = state.schedules.findIndex((item) => item.id === result.id);
        if (index >= 0) state.schedules[index] = result;
        showToast('Schedule updated successfully.', 'success');
      } else {
        result = await window.SCHEL.scheduleApi.createSchedule(payload);
        state.schedules.push(result);
        showToast('Schedule created successfully.', 'success');
      }

      renderScheduleList();
      closeModal(scheduleModal);
      scheduleForm.reset();
      state.pendingCreateId = null;
    } catch (error) {
      showToast('Could not save the schedule.', 'error');
    }
  };

  const handleDeleteConfirmation = async () => {
    if (!state.pendingDeleteId) return;
    try {
      await window.SCHEL.scheduleApi.deleteSchedule(state.pendingDeleteId);
      state.schedules = state.schedules.filter((item) => item.id !== state.pendingDeleteId);
      renderScheduleList();
      closeModal(deleteModal);
      showToast('Schedule deleted successfully.', 'success');
    } catch (error) {
      showToast('Could not delete the schedule.', 'error');
    } finally {
      state.pendingDeleteId = null;
    }
  };

  const refreshSchedules = async () => {
    renderLoading();
    try {
      const schedules = await window.SCHEL.scheduleApi.getSchedules();
      state.schedules = schedules;
      const categoryResult = await window.SCHEL.api.getCategories();
      if (!Array.isArray(categoryResult?.categories)) {
        throw new Error('The server returned an invalid categories response.');
      }
      state.categories = categoryResult.categories;
      populateCategorySelectors();
      renderScheduleList();
    } catch (error) {
      scheduleList.innerHTML = '';
      scheduleList.appendChild(buildEmptyState('Unable to load schedules', 'Please try again in a moment.', 'Try Again', refreshSchedules));
      showToast('Unable to load schedules.', 'error');
    }
  };

  const toggleSortMode = () => {
    if (state.sortMode === 'date-asc') {
      state.sortMode = 'date-desc';
      sortToggle.textContent = 'Sort: Date ↓';
    } else if (state.sortMode === 'date-desc') {
      state.sortMode = 'priority';
      sortToggle.textContent = 'Sort: Priority';
    } else if (state.sortMode === 'priority') {
      state.sortMode = 'newest';
      sortToggle.textContent = 'Sort: Newest';
    } else {
      state.sortMode = 'date-asc';
      sortToggle.textContent = 'Sort: Date ↑';
    }
    renderScheduleList();
  };

  scheduleSearch.addEventListener('input', renderScheduleList);
  scheduleDateFilter.addEventListener('input', renderScheduleList);
  scheduleStatusFilter.addEventListener('change', renderScheduleList);
  schedulePriorityFilter.addEventListener('change', renderScheduleList);
  scheduleCategoryFilter.addEventListener('change', renderScheduleList);
  sortToggle.addEventListener('click', toggleSortMode);
  clearFiltersButton.addEventListener('click', () => {
    scheduleSearch.value = '';
    scheduleDateFilter.value = '';
    scheduleStatusFilter.value = 'ALL';
    schedulePriorityFilter.value = 'ALL';
    scheduleCategoryFilter.value = 'ALL';
    renderScheduleList();
  });
  openCreateModalButton.addEventListener('click', openCreateModal);
  scheduleForm.addEventListener('submit', handleScheduleSubmit);
  document.getElementById('confirm-delete-btn').addEventListener('click', handleDeleteConfirmation);

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

  const categorySelector = document.getElementById('schedule-category');
  categorySelector.replaceChildren(new Option('Loading categories…', ''));
  refreshSchedules();
});
