# Music Player

A modern, responsive web-based music player built with HTML, CSS, and JavaScript.

## Features

- **Play/Pause Control**: Toggle music playback with a single click
- **Track Navigation**: Skip to next or previous track
- **Volume Control**: Adjustable volume slider
- **Progress Bar**: Visual representation of playback progress with click-to-seek functionality
- **Playlist**: Display and select songs from the playlist
- **Keyboard Shortcuts**: 
  - `Space` - Play/Pause
  - `Arrow Right` - Next track
  - `Arrow Left` - Previous track
- **Responsive Design**: Works on desktop and mobile devices
- **Modern UI**: Clean and attractive gradient-based design

## How to Use

1. Create an `audio` folder in the same directory as `index.html`
2. Add your MP3 audio files to the `audio` folder (e.g., `song1.mp3`, `song2.mp3`, etc.)
3. Open `script.js` and update the `playlist` array with your song information
4. Open `index.html` in a web browser
5. Click the play button to start playback
6. Use the controls to navigate between songs, adjust volume, or seek through the track
7. Click on any song in the playlist to play it directly

## Files Structure

- `index.html` - Main HTML structure
- `styles.css` - Styling and layout
- `script.js` - Music player functionality

## Customization

To add your own music:

1. Create an `audio` folder in the same directory as `index.html`
2. Place your MP3 files in the `audio` folder
3. Open `script.js` and find the `playlist` array in the `MusicPlayer` constructor
4. Update the song objects with your song information:
```javascript
{
    title: 'Your Song Title',
    artist: 'Artist Name',
    src: 'audio/your-song-file.mp3'
}
```

**Note:** The audio files should be in a supported format (MP3, OGG, WAV). Make sure the file paths in the `src` property match your actual file locations.

## Browser Compatibility

Works on all modern browsers that support HTML5 audio:
- Chrome
- Firefox
- Safari
- Edge

## License

MIT License