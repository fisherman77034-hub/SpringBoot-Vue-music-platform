const AUDIO_EXT = new Set(['mp3', 'flac', 'wav', 'm4a', 'ogg', 'aac', 'opus', 'webm'])
const IMAGE_EXT = new Set(['jpg', 'jpeg', 'png', 'gif', 'webp'])
const LYRIC_EXT = new Set(['lrc', 'txt'])

export function fileExtension(filename: string): string {
  const name = filename.trim()
  const i = name.lastIndexOf('.')
  if (i < 0 || i >= name.length - 1) return ''
  return name.slice(i + 1).toLowerCase()
}

export function validateAdminAudioFile(f: File): string | null {
  const ext = fileExtension(f.name)
  if (!AUDIO_EXT.has(ext)) {
    return `音频格式不正确，仅支持：${[...AUDIO_EXT].sort().join('、')}`
  }
  return null
}

export function validateAdminCoverFile(f: File): string | null {
  const ext = fileExtension(f.name)
  if (!IMAGE_EXT.has(ext)) {
    return '封面格式不正确，仅支持：jpg、jpeg、png、gif、webp'
  }
  return null
}

export function validateAdminLyricFile(f: File): string | null {
  const ext = fileExtension(f.name)
  if (!LYRIC_EXT.has(ext)) {
    return '歌词格式不正确，仅支持 .lrc 与 .txt'
  }
  return null
}

export function clearFileInput(e: Event) {
  const input = e.target as HTMLInputElement
  input.value = ''
}
