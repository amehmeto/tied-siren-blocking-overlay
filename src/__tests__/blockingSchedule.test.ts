import type { BlockingWindow } from '../TiedSirenBlockingOverlay.types'
import TiedSirenBlockingOverlayModule from '../TiedSirenBlockingOverlayModule'
import {
  setBlockingSchedule,
  getBlockingSchedule,
  clearBlockingSchedule,
} from '../index'

// Mock the native module
jest.mock('../TiedSirenBlockingOverlayModule', () => ({
  __esModule: true,
  default: {
    showOverlay: jest.fn(),
    setBlockedApps: jest.fn(),
    getBlockedApps: jest.fn(),
    clearBlockedApps: jest.fn(),
    setBlockingSchedule: jest.fn(),
    getBlockingSchedule: jest.fn(),
    clearBlockingSchedule: jest.fn(),
  },
}))

const mockSetBlockingSchedule =
  TiedSirenBlockingOverlayModule.setBlockingSchedule as jest.Mock
const mockGetBlockingSchedule =
  TiedSirenBlockingOverlayModule.getBlockingSchedule as jest.Mock
const mockClearBlockingSchedule =
  TiedSirenBlockingOverlayModule.clearBlockingSchedule as jest.Mock

describe('setBlockingSchedule', () => {
  beforeEach(() => {
    jest.clearAllMocks()
  })

  it('should call native module with correct parameters', async () => {
    const windows: BlockingWindow[] = [
      {
        id: 'work-hours',
        startTime: '09:00',
        endTime: '17:00',
        packageNames: ['com.instagram.android', 'com.twitter.android'],
      },
    ]

    mockSetBlockingSchedule.mockResolvedValueOnce(undefined)

    await setBlockingSchedule(windows)

    expect(mockSetBlockingSchedule).toHaveBeenCalledTimes(1)
    expect(mockSetBlockingSchedule).toHaveBeenCalledWith(windows)
  })

  it('should handle multiple windows', async () => {
    const windows: BlockingWindow[] = [
      {
        id: 'work-hours',
        startTime: '09:00',
        endTime: '17:00',
        packageNames: ['com.instagram.android'],
      },
      {
        id: 'sleep-time',
        startTime: '22:00',
        endTime: '06:00',
        packageNames: ['com.facebook.katana'],
      },
    ]

    mockSetBlockingSchedule.mockResolvedValueOnce(undefined)

    await setBlockingSchedule(windows)

    expect(mockSetBlockingSchedule).toHaveBeenCalledWith(windows)
  })

  it('should handle empty array', async () => {
    mockSetBlockingSchedule.mockResolvedValueOnce(undefined)

    await setBlockingSchedule([])

    expect(mockSetBlockingSchedule).toHaveBeenCalledWith([])
  })

  it('should propagate ERR_NO_CONTEXT error from native module', async () => {
    const error = new Error('Context not available')
    ;(error as any).code = 'ERR_NO_CONTEXT'
    mockSetBlockingSchedule.mockRejectedValueOnce(error)

    await expect(
      setBlockingSchedule([
        {
          id: 'test',
          startTime: '09:00',
          endTime: '17:00',
          packageNames: ['com.example.app'],
        },
      ]),
    ).rejects.toThrow('Context not available')
  })

  it('should propagate ERR_INVALID_SCHEDULE error from native module', async () => {
    const error = new Error('Invalid schedule data')
    ;(error as any).code = 'ERR_INVALID_SCHEDULE'
    mockSetBlockingSchedule.mockRejectedValueOnce(error)

    await expect(
      setBlockingSchedule([
        {
          id: 'test',
          startTime: 'invalid',
          endTime: '17:00',
          packageNames: [],
        },
      ]),
    ).rejects.toThrow('Invalid schedule data')
  })

  it('should propagate ERR_SCHEDULE_FAILED error from native module', async () => {
    const error = new Error('Failed to set schedule')
    ;(error as any).code = 'ERR_SCHEDULE_FAILED'
    mockSetBlockingSchedule.mockRejectedValueOnce(error)

    await expect(
      setBlockingSchedule([
        {
          id: 'test',
          startTime: '09:00',
          endTime: '17:00',
          packageNames: [],
        },
      ]),
    ).rejects.toThrow('Failed to set schedule')
  })
})

describe('getBlockingSchedule', () => {
  beforeEach(() => {
    jest.clearAllMocks()
  })

  it('should return schedule from native module', async () => {
    const expectedWindows: BlockingWindow[] = [
      {
        id: 'work-hours',
        startTime: '09:00',
        endTime: '17:00',
        packageNames: ['com.instagram.android'],
      },
    ]
    mockGetBlockingSchedule.mockResolvedValueOnce(expectedWindows)

    const result = await getBlockingSchedule()

    expect(mockGetBlockingSchedule).toHaveBeenCalledTimes(1)
    expect(result).toEqual(expectedWindows)
  })

  it('should return empty array when no schedule is set', async () => {
    mockGetBlockingSchedule.mockResolvedValueOnce([])

    const result = await getBlockingSchedule()

    expect(result).toEqual([])
  })

  it('should propagate ERR_NO_CONTEXT error from native module', async () => {
    const error = new Error('Context not available')
    ;(error as any).code = 'ERR_NO_CONTEXT'
    mockGetBlockingSchedule.mockRejectedValueOnce(error)

    await expect(getBlockingSchedule()).rejects.toThrow('Context not available')
  })
})

describe('clearBlockingSchedule', () => {
  beforeEach(() => {
    jest.clearAllMocks()
  })

  it('should call native module', async () => {
    mockClearBlockingSchedule.mockResolvedValueOnce(undefined)

    await clearBlockingSchedule()

    expect(mockClearBlockingSchedule).toHaveBeenCalledTimes(1)
  })

  it('should propagate ERR_NO_CONTEXT error from native module', async () => {
    const error = new Error('Context not available')
    ;(error as any).code = 'ERR_NO_CONTEXT'
    mockClearBlockingSchedule.mockRejectedValueOnce(error)

    await expect(clearBlockingSchedule()).rejects.toThrow(
      'Context not available',
    )
  })
})

describe('BlockingWindow type', () => {
  it('should accept valid window with all required fields', () => {
    const window: BlockingWindow = {
      id: 'test-window',
      startTime: '09:00',
      endTime: '17:00',
      packageNames: ['com.example.app'],
    }

    expect(window.id).toBe('test-window')
    expect(window.startTime).toBe('09:00')
    expect(window.endTime).toBe('17:00')
    expect(window.packageNames).toEqual(['com.example.app'])
  })

  it('should accept overnight window (end time before start time)', () => {
    const window: BlockingWindow = {
      id: 'overnight',
      startTime: '22:00',
      endTime: '06:00',
      packageNames: ['com.night.app'],
    }

    expect(window.startTime).toBe('22:00')
    expect(window.endTime).toBe('06:00')
  })

  it('should accept window with empty package list', () => {
    const window: BlockingWindow = {
      id: 'empty-window',
      startTime: '00:00',
      endTime: '23:59',
      packageNames: [],
    }

    expect(window.packageNames).toEqual([])
  })

  it('should accept window with multiple packages', () => {
    const window: BlockingWindow = {
      id: 'multi-package',
      startTime: '09:00',
      endTime: '17:00',
      packageNames: [
        'com.facebook.katana',
        'com.instagram.android',
        'com.twitter.android',
        'com.snapchat.android',
      ],
    }

    expect(window.packageNames.length).toBe(4)
  })
})
