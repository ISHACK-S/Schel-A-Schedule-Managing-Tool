document.addEventListener('DOMContentLoaded', () => {
  const forms = document.querySelectorAll('[data-form]');

  const setFieldError = (input, message) => {
    const group = input.closest('.field-group');
    const error = group?.querySelector('.field-error');
    if (error) {
      error.textContent = message || '';
    }
    input.setAttribute('aria-invalid', message ? 'true' : 'false');
    input.classList.toggle('field-invalid', Boolean(message));
  };

  const clearFormErrors = (form) => {
    form.querySelectorAll('.field-error').forEach((node) => {
      node.textContent = '';
    });
    form.querySelectorAll('input').forEach((input) => {
      input.classList.remove('field-invalid');
      input.setAttribute('aria-invalid', 'false');
    });
    const alertBox = form.querySelector('.form-alert');
    if (alertBox) {
      alertBox.className = 'form-alert';
      alertBox.hidden = true;
      alertBox.textContent = '';
    }
  };

  const showFormMessage = (form, type, message) => {
    const alertBox = form.querySelector('.form-alert');
    if (!alertBox) return;
    alertBox.className = `form-alert ${type}`;
    alertBox.hidden = false;
    alertBox.textContent = message;
  };

  const getFormValues = (form) => Object.fromEntries(new FormData(form).entries());

  const validateEmail = (value) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);

  const validateLogin = (values) => {
    const errors = {};
    const identifier = String(values.identifier || '').trim();
    const password = String(values.password || '');

    if (!identifier) {
      errors.identifier = 'Email or username is required.';
    } else if (identifier.includes('@') && !validateEmail(identifier)) {
      errors.identifier = 'Enter a valid email address.';
    }

    if (!password) {
      errors.password = 'Password is required.';
    } else if (password.length < 6) {
      errors.password = 'Password must be at least 6 characters.';
    }

    return errors;
  };

  const validateRegister = (values) => {
    const errors = {};
    const username = String(values.username || '').trim();
    const email = String(values.email || '').trim();
    const password = String(values.password || '');
    const confirmPassword = String(values.confirmPassword || '');

    if (!username) {
      errors.username = 'Username is required.';
    } else if (username.length < 3) {
      errors.username = 'Username must be at least 3 characters.';
    }

    if (!email) {
      errors.email = 'Email is required.';
    } else if (!validateEmail(email)) {
      errors.email = 'Enter a valid email address.';
    }

    if (!password) {
      errors.password = 'Password is required.';
    } else if (password.length < 8) {
      errors.password = 'Use at least 8 characters.';
    }

    if (!confirmPassword) {
      errors.confirmPassword = 'Please confirm your password.';
    } else if (password !== confirmPassword) {
      errors.confirmPassword = 'Passwords do not match.';
    }

    return errors;
  };

  const attachPasswordToggle = (input) => {
    const wrapper = input.closest('.password-field');
    if (!wrapper) return;

    const toggle = wrapper.querySelector('[data-password-toggle]');
    if (!toggle) return;

    toggle.addEventListener('click', () => {
      const isHidden = input.type === 'password';
      input.type = isHidden ? 'text' : 'password';
      toggle.textContent = isHidden ? 'Hide' : 'Show';
      toggle.setAttribute('aria-label', `${isHidden ? 'Hide' : 'Show'} password`);
    });
  };

  forms.forEach((form) => {
    const formType = form.dataset.form;
    const submitButton = form.querySelector('button[type="submit"]');
    const originalText = submitButton.textContent;

    form.querySelectorAll('input').forEach((input) => attachPasswordToggle(input));

    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      clearFormErrors(form);

      const values = getFormValues(form);
      const validationErrors = formType === 'login' ? validateLogin(values) : validateRegister(values);

      const inputs = form.querySelectorAll('input');
      inputs.forEach((input) => {
        const fieldName = input.name;
        if (validationErrors[fieldName]) {
          setFieldError(input, validationErrors[fieldName]);
        }
      });

      if (Object.keys(validationErrors).length > 0) {
        showFormMessage(form, 'error', formType === 'login' ? 'Invalid email or password.' : 'Please review the highlighted fields.');
        return;
      }

      submitButton.disabled = true;
      submitButton.classList.add('is-loading');
      submitButton.innerHTML = '<span class="loader" aria-label="Loading"></span><span>Loading</span>';
      showFormMessage(form, 'info', formType === 'login' ? 'Checking your account…' : 'Creating your account…');

      try {
        const method = formType === 'login' ? window.SCHEL.authApi.login : window.SCHEL.authApi.register;
        const result = await method(values);

        if (formType === 'register') {
          showFormMessage(form, 'success', 'Account created successfully. Redirecting to sign in…');
          setTimeout(() => {
            window.location.href = 'login.html';
          }, 800);
          return;
        }

        window.SCHEL.setCurrentUser(result.user);
        showFormMessage(form, 'success', 'Welcome back. Redirecting to your dashboard…');

        setTimeout(() => {
          window.location.href = 'dashboard.html';
        }, 500);
      } catch (error) {
        console.error('Authentication error:', error);
        showFormMessage(form, 'error', error?.message || 'Please check your connection and try again.');
      } finally {
        submitButton.disabled = false;
        submitButton.classList.remove('is-loading');
        submitButton.innerHTML = originalText;
      }
    });
  });

  const redirectIfAuthenticated = () => {
    if (window.location.pathname.endsWith('login.html') || window.location.pathname.endsWith('register.html')) {
      if (window.SCHEL.isAuthenticated()) {
        window.location.href = 'dashboard.html';
      }
    }
  };

  redirectIfAuthenticated();
});
