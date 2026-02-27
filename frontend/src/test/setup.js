import '@testing-library/jest-dom';

// Mock ResizeObserver (needed by Ant Design components)
global.ResizeObserver = class ResizeObserver {
  constructor(cb) { this.cb = cb; }
  observe() {}
  unobserve() {}
  disconnect() {}
};

// Mock window.matchMedia (needed by Ant Design)
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation(query => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })),
});

// Mock window.getComputedStyle (needed by Ant Design)
const originalGetComputedStyle = window.getComputedStyle;
window.getComputedStyle = (elt, pseudoElt) => {
  try {
    return originalGetComputedStyle(elt, pseudoElt);
  } catch {
    return {};
  }
};

// Suppress Ant Design console warnings in tests
const originalWarn = console.warn;
const originalError = console.error;
beforeAll(() => {
  console.warn = (...args) => {
    if (args[0]?.toString().includes('[antd')) return;
    originalWarn.call(console, ...args);
  };
  console.error = (...args) => {
    if (args[0]?.toString().includes('[antd')) return;
    if (args[0]?.toString().includes('Warning:')) return;
    originalError.call(console, ...args);
  };
});

afterAll(() => {
  console.warn = originalWarn;
  console.error = originalError;
});
