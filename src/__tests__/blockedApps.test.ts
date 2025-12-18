import TiedSirenBlockingOverlayModule from '../TiedSirenBlockingOverlayModule'
import {
  setBlockedApps,
  getBlockedApps,
  clearBlockedApps,
  BLOCKING_CALLBACK_CLASS,
} from '../index'

// Mock the native module
jest.mock('../TiedSirenBlockingOverlayModule', () => ({
  __esModule: true,
  default: {
    showOverlay: jest.fn(),
    setBlockedApps: jest.fn(),
    getBlockedApps: jest.fn(),
    clearBlockedApps: jest.fn(),
  },
}))

const mockSetBlockedApps =
  TiedSirenBlockingOverlayModule.setBlockedApps as jest.Mock
const mockGetBlockedApps =
  TiedSirenBlockingOverlayModule.getBlockedApps as jest.Mock
const mockClearBlockedApps =
  TiedSirenBlockingOverlayModule.clearBlockedApps as jest.Mock

describe('setBlockedApps', () => {
  beforeEach(() => {
    jest.clearAllMocks()
  })

  it('should call native module with correct parameters', async () => {
    const packageNames = ['com.facebook.katana', 'com.instagram.android']

    mockSetBlockedApps.mockResolvedValueOnce(undefined)

    await setBlockedApps(packageNames)

    expect(mockSetBlockedApps).toHaveBeenCalledTimes(1)
    expect(mockSetBlockedApps).toHaveBeenCalledWith(packageNames)
  })

  it('should handle empty array', async () => {
    mockSetBlockedApps.mockResolvedValueOnce(undefined)

    await setBlockedApps([])

    expect(mockSetBlockedApps).toHaveBeenCalledWith([])
  })

  it('should propagate ERR_NO_CONTEXT error from native module', async () => {
    const error = new Error('Context not available')
    ;(error as any).code = 'ERR_NO_CONTEXT'
    mockSetBlockedApps.mockRejectedValueOnce(error)

    await expect(setBlockedApps(['com.example.app'])).rejects.toThrow(
      'Context not available',
    )
  })
})

describe('getBlockedApps', () => {
  beforeEach(() => {
    jest.clearAllMocks()
  })

  it('should return blocked apps from native module', async () => {
    const expectedApps = ['com.facebook.katana', 'com.instagram.android']
    mockGetBlockedApps.mockResolvedValueOnce(expectedApps)

    const result = await getBlockedApps()

    expect(mockGetBlockedApps).toHaveBeenCalledTimes(1)
    expect(result).toEqual(expectedApps)
  })

  it('should return empty array when no apps are blocked', async () => {
    mockGetBlockedApps.mockResolvedValueOnce([])

    const result = await getBlockedApps()

    expect(result).toEqual([])
  })

  it('should propagate ERR_NO_CONTEXT error from native module', async () => {
    const error = new Error('Context not available')
    ;(error as any).code = 'ERR_NO_CONTEXT'
    mockGetBlockedApps.mockRejectedValueOnce(error)

    await expect(getBlockedApps()).rejects.toThrow('Context not available')
  })
})

describe('clearBlockedApps', () => {
  beforeEach(() => {
    jest.clearAllMocks()
  })

  it('should call native module', async () => {
    mockClearBlockedApps.mockResolvedValueOnce(undefined)

    await clearBlockedApps()

    expect(mockClearBlockedApps).toHaveBeenCalledTimes(1)
  })

  it('should propagate ERR_NO_CONTEXT error from native module', async () => {
    const error = new Error('Context not available')
    ;(error as any).code = 'ERR_NO_CONTEXT'
    mockClearBlockedApps.mockRejectedValueOnce(error)

    await expect(clearBlockedApps()).rejects.toThrow('Context not available')
  })
})

describe('BLOCKING_CALLBACK_CLASS', () => {
  it('should have correct fully qualified class name', () => {
    expect(BLOCKING_CALLBACK_CLASS).toBe(
      'expo.modules.blockingoverlay.BlockingCallback',
    )
  })
})
