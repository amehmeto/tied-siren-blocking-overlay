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
    const blockUntil = Date.now() + 3600000

    mockShowOverlay.mockResolvedValueOnce(undefined)

    await showOverlay(packageName, blockUntil)

    expect(mockShowOverlay).toHaveBeenCalledTimes(1)
    expect(mockShowOverlay).toHaveBeenCalledWith(packageName, blockUntil)
  })

  it('should propagate ERR_INVALID_PACKAGE error from native module', async () => {
    const error = new Error('Package name cannot be empty')
    ;(error as any).code = 'ERR_INVALID_PACKAGE'
    mockShowOverlay.mockRejectedValueOnce(error)

    await expect(showOverlay('', Date.now())).rejects.toThrow(
      'Package name cannot be empty',
    )
  })

  it('should propagate ERR_OVERLAY_LAUNCH error from native module', async () => {
    const error = new Error('Failed to launch overlay')
    ;(error as any).code = 'ERR_OVERLAY_LAUNCH'
    mockShowOverlay.mockRejectedValueOnce(error)

    await expect(showOverlay('com.example.app', Date.now())).rejects.toThrow(
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
      await showOverlay(packageName, Date.now() + 1000)
      expect(mockShowOverlay).toHaveBeenLastCalledWith(
        packageName,
        expect.any(Number),
      )
    }
  })

  it('should accept blockUntil as future timestamp', async () => {
    mockShowOverlay.mockResolvedValue(undefined)

    const futureTimestamp = Date.now() + 60 * 60 * 1000 // 1 hour from now

    await showOverlay('com.example.app', futureTimestamp)

    expect(mockShowOverlay).toHaveBeenCalledWith(
      'com.example.app',
      futureTimestamp,
    )
  })
})
