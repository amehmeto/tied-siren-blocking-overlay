// Mock expo-modules-core
jest.mock('expo-modules-core', () => ({
  requireNativeModule: jest.fn(() => ({
    showOverlay: jest.fn(),
  })),
}))
