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

1. Open `index.html` in a web browser
2. The player will load with a default playlist of sample songs
3. Click the play button to start playback
4. Use the controls to navigate between songs, adjust volume, or seek through the track
5. Click on any song in the playlist to play it directly

## Files Structure

- `index.html` - Main HTML structure
- `styles.css` - Styling and layout
- `script.js` - Music player functionality

## Customization

To add your own music:

1. Open `script.js`
2. Find the `playlist` array in the `MusicPlayer` constructor
3. Add your song objects with the following format:
```javascript
{
    title: 'Song Title',
    artist: 'Artist Name',
    src: 'path/to/audio/file.mp3'
}
```

## Browser Compatibility

Works on all modern browsers that support HTML5 audio:
- Chrome
- Firefox
- Safari
- Edge

## License

MIT License