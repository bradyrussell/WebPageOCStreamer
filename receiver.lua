function bytesToInt(bytes)
    return bit32.bor(bit32.lshift(bit32.band(string.byte(bytes, 1) , 0xFF) , 24), bit32.bor(bit32.lshift(bit32.band(string.byte(bytes, 2) , 0xFF) , 16), bit32.bor(bit32.lshift(bit32.band(string.byte(bytes, 3) , 0xFF) , 8), bit32.band(string.byte(bytes, 4) , 0xFF))));
end
function RGBToInt(r, g, b)
    return bit32.bor(bit32.lshift(bit32.band(r , 0xFF) , 16), bit32.bor(bit32.lshift(bit32.band(g , 0xFF) , 8), bit32.band(b , 0xFF)));
end

local component = require "component"
local gpu = component.gpu
local internet = require("internet")  
local handle = internet.open("127.0.0.1", 54321)  

while true do
 
  local frameSize = handle:read(4)
  local frameBytes = bytesToInt(frameSize)
 
  local width = 160

  local x = 1
  local y = 1

  for i = 1,frameBytes,3 do 
    local r = string.byte(handle:read(1), 1)
    local g = string.byte(handle:read(1), 1)
    local b = string.byte(handle:read(1), 1)
    ---print(x..', '..y..': '..r..', '..g..', '..b)

    gpu.setForeground(RGBToInt(r,g,b))
    gpu.fill(x, y, 1, 1, "▓")

    x = x + 1
    if x > width then
      x = 1
      y = y + 1
    end
  end
end 
handle:close()
