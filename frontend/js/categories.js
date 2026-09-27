document.addEventListener('DOMContentLoaded', () => {
  if (!window.SCHEL.requireAuth()) {
    return;
  }

  const categoryList = document.getElementById('category-list');
  const categorySearch = document.getElementById('category-search');
  const categoryCountBadge = document.getElementById('category-count-badge');
  const categoryModal = document.getElementById('category-modal');
  const deleteCategoryModal = document.getElementById('delete-category-modal');
  const categoryForm = document.getElementById('category-form');
  const toastContainer = document.getElementById('toast-container');

  const state = {
    categories: [],
    pendingEditId: null,
    pendingDeleteId: null
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

  const buildEmptyState = (title, message, actionText, actionHandler) => {
    const wrapper = document.createElement('div');
    wrapper.className = 'empty-state';
    wrapper.innerHTML = `
      <div class="empty-icon">🏷️</div>
      <h3>${title}</h3>
      <p>${message}</p>
      <button class="btn btn-primary" type="button">${actionText}</button>
    `;
    wrapper.querySelector('button').addEventListener('click', actionHandler);
    return wrapper;
  };

  const renderLoading = () => {
    categoryList.innerHTML = '';
    const skeletons = Array.from({ length: 3 }, () => {
      const card = document.createElement('div');
      card.className = 'category-card loading-card';
      card.innerHTML = `
        <div class="load-skeleton" style="height: 18px; width: 42%; margin-bottom: 14px;"></div>
        <div class="load-skeleton" style="height: 12px; width: 72%; margin-bottom: 18px;"></div>
        <div class="load-skeleton" style="height: 32px; width: 120px; border-radius: 999px;"></div>
      `;
      return card;
    });
    skeletons.forEach((card) => categoryList.appendChild(card));
  };

  const categoryScheduleCount = async (categoryId, categoryName) => {
    try {
      const schedules = await window.SCHEL.scheduleApi.getSchedules();
      return schedules.filter((schedule) => {
        const matchesId = String(schedule.category_id || '') === String(categoryId);
        const matchesName = (schedule.category || 'General') === (categoryName || 'General');
        return matchesId || matchesName;
      }).length;
    } catch (error) {
      return 0;
    }
  };

  const renderCategoryList = async () => {
    const searchTerm = (categorySearch.value || '').trim().toLowerCase();
    const visible = state.categories.filter((category) => {
      const haystack = `${category.name || ''}`.toLowerCase();
      return !searchTerm || haystack.includes(searchTerm);
    });

    categoryCountBadge.textContent = `${visible.length} ${visible.length === 1 ? 'category' : 'categories'}`;
    categoryList.innerHTML = '';

    if (visible.length === 0) {
      const hasData = state.categories.length > 0;
      const title = hasData ? 'No matching categories' : 'No categories yet';
      const message = hasData ? 'Try a different search.' : 'Create categories to keep your schedules organized.';
      const buttonText = hasData ? 'Clear Search' : '+ Create Category';
      const actionHandler = hasData ? () => {
        categorySearch.value = '';
        renderCategoryList();
      } : () => openCreateModal();
      categoryList.appendChild(buildEmptyState(title, message, buttonText, actionHandler));
      return;
    }

    for (const category of visible) {
      const count = await categoryScheduleCount(category.id, category.name);
      const card = document.createElement('article');
      card.className = 'category-card';
      card.innerHTML = `
        <div class="category-header">
          <div class="category-title-wrap">
            <span class="category-icon" style="background:${category.color || '#5b6cff'};"></span>
            <div>
              <h3>${category.name || 'Untitled category'}</h3>
            </div>
          </div>
        </div>
        <div class="category-body">
          <p>${count} ${count === 1 ? 'schedule' : 'schedules'}</p>
        </div>
        <div class="category-footer">
          <button class="btn btn-secondary btn-small" type="button" data-edit-id="${category.id}">Edit</button>
          <button class="btn btn-danger btn-small" type="button" data-delete-id="${category.id}">Delete</button>
        </div>
      `;

      card.querySelector('[data-edit-id]').addEventListener('click', () => openEditModal(category.id));
      card.querySelector('[data-delete-id]').addEventListener('click', () => openDeleteModal(category.id));
      categoryList.appendChild(card);
    }
  };

  const openCreateModal = () => {
    clearFormErrors();
    categoryForm.reset();
    document.getElementById('category-modal-title').textContent = 'Create Category';
    document.getElementById('save-category-btn').textContent = 'Create Category';
    document.getElementById('category-color').value = '#5b6cff';
    state.pendingEditId = null;
    categoryModal.classList.add('open');
    categoryModal.setAttribute('aria-hidden', 'false');
  };

  const openEditModal = (id) => {
    const category = state.categories.find((item) => item.id === id);
    if (!category) return;
    clearFormErrors();
    state.pendingEditId = id;
    document.getElementById('category-modal-title').textContent = 'Edit Category';
    document.getElementById('save-category-btn').textContent = 'Update Category';
    document.getElementById('category-name').value = category.name || '';
    document.getElementById('category-color').value = category.color || '#5b6cff';
    categoryModal.classList.add('open');
    categoryModal.setAttribute('aria-hidden', 'false');
  };

  const closeModal = (modal) => {
    modal.classList.remove('open');
    modal.setAttribute('aria-hidden', 'true');
  };

  const closeAllModals = () => {
    closeModal(categoryModal);
    closeModal(deleteCategoryModal);
  };

  const openDeleteModal = (id) => {
    const category = state.categories.find((item) => item.id === id);
    if (!category) return;
    state.pendingDeleteId = id;
    document.getElementById('delete-category-message').textContent = `Are you sure you want to delete "${category.name || 'this category'}"?`;
    deleteCategoryModal.classList.add('open');
    deleteCategoryModal.setAttribute('aria-hidden', 'false');
  };

  const validateCategoryPayload = (payload) => {
    const errors = {};
    const name = (payload.name || '').trim();
    if (!name) {
      errors.name = 'Category name is required.';
    } else if (name.length > 100) {
      errors.name = 'Category name cannot exceed 100 characters.';
    }
    if (payload.color && !/^#[0-9A-Fa-f]{3}([0-9A-Fa-f]{3})?$/.test(payload.color)) {
      errors.color = 'Please provide a valid color.';
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
    const formData = new FormData(categoryForm);
    const payload = {
      id: state.pendingEditId || ('category-' + Date.now()),
      name: formData.get('name') || '',
      color: formData.get('color') || '#5b6cff',
      user_id: window.SCHEL.getCurrentUser()?.id || 'local-user'
    };

    const errors = validateCategoryPayload(payload);
    if (Object.keys(errors).length) {
      applyFormErrors(errors);
      return;
    }

    try {
      if (state.pendingEditId) {
        await window.SCHEL.categoryApi.updateCategory(payload);
        showToast('Category updated successfully.', 'success');
      } else {
        await window.SCHEL.categoryApi.createCategory(payload);
        showToast('Category created successfully.', 'success');
      }

      categoryForm.reset();
      clearFormErrors();
      state.pendingEditId = null;
      closeModal(categoryModal);
      await refreshCategories();
    } catch (error) {
      showToast('Could not save this category.', 'error');
    }
  };

  const handleDelete = async () => {
    if (!state.pendingDeleteId) return;

    try {
      const schedules = await window.SCHEL.scheduleApi.getSchedules();
      const inUse = schedules.some((schedule) => {
        const sameCategoryId = String(schedule.category_id || '') === String(state.pendingDeleteId);
        const sameCategoryName = (schedule.category || 'General') === state.categories.find((c) => c.id === state.pendingDeleteId)?.name;
        return sameCategoryId || sameCategoryName;
      });

      if (inUse) {
        showToast('This category cannot be deleted while it is being used by existing schedules.', 'error');
        closeModal(deleteCategoryModal);
        state.pendingDeleteId = null;
        return;
      }

      await window.SCHEL.categoryApi.deleteCategory(state.pendingDeleteId);
      showToast('Category deleted successfully.', 'success');
      closeModal(deleteCategoryModal);
      state.pendingDeleteId = null;
      await refreshCategories();
    } catch (error) {
      showToast('Could not delete this category.', 'error');
    }
  };

  const refreshCategories = async () => {
    renderLoading();
    try {
      state.categories = await window.SCHEL.categoryApi.getCategories();
      await renderCategoryList();
    } catch (error) {
      categoryList.innerHTML = '';
      categoryList.appendChild(buildEmptyState('Unable to load categories', 'Please try again in a moment.', 'Try Again', refreshCategories));
      showToast('Unable to load categories.', 'error');
    }
  };

  categorySearch.addEventListener('input', renderCategoryList);
  categoryForm.addEventListener('submit', handleSubmit);
  document.getElementById('open-create-modal').addEventListener('click', openCreateModal);
  document.getElementById('confirm-delete-category-btn').addEventListener('click', handleDelete);

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

  refreshCategories();
});
