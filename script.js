// Music Player JavaScript

class MusicPlayer {
    constructor() {
        this.audio = document.getElementById('audio');
        this.playBtn = document.getElementById('play-btn');
        this.prevBtn = document.getElementById('prev-btn');
        this.nextBtn = document.getElementById('next-btn');
        this.volumeSlider = document.getElementById('volume-slider');
        this.progressBar = document.querySelector('.progress-bar');
        this.progress = document.querySelector('.progress');
        this.currentTimeEl = document.querySelector('.current-time');
        this.durationTimeEl = document.querySelector('.duration-time');
        this.songTitle = document.querySelector('.song-title');
        this.artistName = document.querySelector('.artist-name');
        this.playlistEl = document.getElementById('playlist');
        this.playIcon = document.querySelector('.play-icon');
        this.pauseIcon = document.querySelector('.pause-icon');
        
        this.currentSongIndex = 0;
        this.isPlaying = false;
        
        // Sample playlist - Note: Replace with your own audio files
        // IMPORTANT: If serving your page over HTTPS, audio files must also be HTTPS
        // or hosted on the same domain to avoid mixed content security blocks
        this.playlist = [
            {
                title: 'Summer Vibes',
                artist: 'Artist 1',
                src: 'audio/song1.mp3'  // Replace with actual audio file path
            },
            {
                title: 'Chill Beats',
                artist: 'Artist 2',
                src: 'audio/song2.mp3'  // Replace with actual audio file path
            },
            {
                title: 'Night Drive',
                artist: 'Artist 3',
                src: 'audio/song3.mp3'  // Replace with actual audio file path
            },
            {
                title: 'Acoustic Dreams',
                artist: 'Artist 4',
                src: 'audio/song4.mp3'  // Replace with actual audio file path
            },
            {
                title: 'Electronic Waves',
                artist: 'Artist 5',
                src: 'audio/song5.mp3'  // Replace with actual audio file path
            }
        ];
        
        this.init();
    }
    
    init() {
        this.renderPlaylist();
        this.loadSong(0);
        this.setupEventListeners();
        
        // Set initial volume
        this.audio.volume = this.volumeSlider.value / 100;
    }
    
    setupEventListeners() {
        // Play/Pause button
        this.playBtn.addEventListener('click', () => this.togglePlay());
        
        // Previous and Next buttons
        this.prevBtn.addEventListener('click', () => this.prevSong());
        this.nextBtn.addEventListener('click', () => this.nextSong());
        
        // Volume control
        this.volumeSlider.addEventListener('input', (e) => {
            this.audio.volume = e.target.value / 100;
        });
        
        // Progress bar
        this.progressBar.addEventListener('click', (e) => {
            if (!isNaN(this.audio.duration)) {
                const rect = this.progressBar.getBoundingClientRect();
                const percent = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width));
                this.audio.currentTime = percent * this.audio.duration;
            }
        });
        
        // Audio events
        this.audio.addEventListener('timeupdate', () => this.updateProgress());
        this.audio.addEventListener('loadedmetadata', () => this.updateDuration());
        this.audio.addEventListener('ended', () => this.nextSong());
        
        // Keyboard controls
        document.addEventListener('keydown', (e) => {
            if (e.code === 'Space') {
                e.preventDefault();
                this.togglePlay();
            } else if (e.code === 'ArrowRight') {
                this.nextSong();
            } else if (e.code === 'ArrowLeft') {
                this.prevSong();
            }
        });
    }
    
    renderPlaylist() {
        this.playlistEl.innerHTML = '';
        this.playlist.forEach((song, index) => {
            const li = document.createElement('li');
            li.className = 'playlist-item';
            if (index === this.currentSongIndex) {
                li.classList.add('active');
            }
            
            const titleDiv = document.createElement('div');
            titleDiv.className = 'playlist-item-title';
            titleDiv.textContent = String(song.title || 'Unknown Title');
            
            const artistDiv = document.createElement('div');
            artistDiv.className = 'playlist-item-artist';
            artistDiv.textContent = String(song.artist || 'Unknown Artist');
            
            li.appendChild(titleDiv);
            li.appendChild(artistDiv);
            li.addEventListener('click', () => this.loadSong(index, true));
            this.playlistEl.appendChild(li);
        });
    }
    
    loadSong(index, autoPlay = false) {
        this.currentSongIndex = index;
        const song = this.playlist[index];
        
        // Validate and sanitize the audio source
        // Only allow relative paths from audio directory to prevent SSRF attacks
        if (song.src && typeof song.src === 'string' && song.src.match(/^audio\//i)) {
            this.audio.src = song.src;
        } else {
            console.error('Invalid audio source (must start with "audio/"):', song.src);
            this.audio.src = '';
        }
        
        // Validate and set song metadata with fallbacks
        this.songTitle.textContent = String(song.title || 'Unknown Title');
        this.artistName.textContent = String(song.artist || 'Unknown Artist');
        
        this.renderPlaylist();
        
        if (autoPlay) {
            this.play();
        }
    }
    
    togglePlay() {
        if (this.isPlaying) {
            this.pause();
        } else {
            this.play();
        }
    }
    
    play() {
        const playPromise = this.audio.play();
        if (playPromise !== undefined) {
            playPromise.then(() => {
                this.isPlaying = true;
                this.playIcon.style.display = 'none';
                this.pauseIcon.style.display = 'block';
            }).catch(error => {
                console.error('Playback failed:', error);
                this.isPlaying = false;
            });
        }
    }
    
    pause() {
        this.audio.pause();
        this.isPlaying = false;
        this.playIcon.style.display = 'block';
        this.pauseIcon.style.display = 'none';
    }
    
    prevSong() {
        this.currentSongIndex = (this.currentSongIndex - 1 + this.playlist.length) % this.playlist.length;
        this.loadSong(this.currentSongIndex, this.isPlaying);
    }
    
    nextSong() {
        this.currentSongIndex = (this.currentSongIndex + 1) % this.playlist.length;
        this.loadSong(this.currentSongIndex, this.isPlaying);
    }
    
    updateProgress() {
        if (this.audio.duration) {
            const percent = (this.audio.currentTime / this.audio.duration) * 100;
            this.progress.style.width = percent + '%';
            this.currentTimeEl.textContent = this.formatTime(this.audio.currentTime);
        }
    }
    
    updateDuration() {
        this.durationTimeEl.textContent = this.formatTime(this.audio.duration);
    }
    
    formatTime(seconds) {
        if (isNaN(seconds) || seconds < 0 || !isFinite(seconds)) return '0:00';
        const mins = Math.floor(seconds / 60);
        const secs = Math.floor(seconds % 60);
        return `${mins}:${secs.toString().padStart(2, '0')}`;
    }
}

// Initialize the music player when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    new MusicPlayer();
});
