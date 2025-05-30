const eventBus = {
  on(event, callback) {
    document.addEventListener(event, (e) => callback(e.detail));
  },
  off(event, callback) {
    document.removeEventListener(event, callback);
  },
  emit(event, data) {
    document.dispatchEvent(new CustomEvent(event, { detail: data }));
  },
};

export default eventBus;
