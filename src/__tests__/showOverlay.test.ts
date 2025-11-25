import TiedSirenBlockingOverlayModule from '../TiedSirenBlockingOverlayModule'
import { showOverlay } from '../index'

// Mock the native module
jest.mock('../TiedSirenBlockingOverlayModule', () => ({
  __esModule: true,
  default: {
    showOverlay: jest.fn(),
  },
}))

const mockShowOverlay = TiedSirenBlockingOverlayModule.showOverlay as jest.Mock

describe('showOverlay', () => {
  beforeEach(() => {
    jest.clearAllMocks()
  })

  it('should call native module with correct parameters', async () => {
    const packageName = 'com.example.app'

    mockShowOverlay.mockResolvedValueOnce(undefined)

    await showOverlay(packageName)

    expect(mockShowOverlay).toHaveBeenCalledTimes(1)
    expect(mockShowOverlay).toHaveBeenCalledWith(packageName)
  })

  it('should propagate ERR_INVALID_PACKAGE error from native module', async () => {
    const error = new Error('Package name cannot be empty')
    ;(error as any).code = 'ERR_INVALID_PACKAGE'
    mockShowOverlay.mockRejectedValueOnce(error)

    await expect(showOverlay('')).rejects.toThrow('Package name cannot be empty')
  })

  it('should propagate ERR_OVERLAY_LAUNCH error from native module', async () => {
    const error = new Error('Failed to launch overlay')
    ;(error as any).code = 'ERR_OVERLAY_LAUNCH'
    mockShowOverlay.mockRejectedValueOnce(error)

    await expect(showOverlay('com.example.app')).rejects.toThrow(
      'Failed to launch overlay',
    )
  })

  it('should accept valid package names', async () => {
    mockShowOverlay.mockResolvedValue(undefined)

    const validPackageNames = [
      'com.facebook.katana',
      'com.instagram.android',
      'com.twitter.android',
      'org.example.myapp',
    ]

    for (const packageName of validPackageNames) {
      await showOverlay(packageName)
      expect(mockShowOverlay).toHaveBeenLastCalledWith(packageName)
    }
  })
})
