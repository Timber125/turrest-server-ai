Intructions:

- Not all players can place buildings - not sure what's going on there. Find out and fix it.
- The color of the contours around land does not match the players chosen color - the color indication at the right-top of the playing screen also doesnt match the selected color in the lobby. 
- !! The color should be a general "Player" attribute and the lobby selection should set this. In the game, all player-indicating-colors should refer to the player's color - don't duplicate any colors in code. Cleanup where this happened. 
- Make the "ready" checkbox and "color" selection more fashionable, it looks really quick-patched now, make it nicer.
- When a client has old (invalid) session data (invalid token) - he is redirected to /lobby and i think that because of this redirect, the "logout on invalid token" is not triggered properly. Fix please - only route to /lobby when token is valid 
