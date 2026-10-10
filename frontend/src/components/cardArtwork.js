// Only safe display fields are drawn here. Purchase inputs never reach this canvas.
/** @param {string} label @param {string} maskedNumber @param {string} expiry @param {boolean} back */
export function drawCardArtwork(label, maskedNumber, expiry, back) {
  const canvas = document.createElement('canvas')
  canvas.width = 1024
  canvas.height = 646
  const context = canvas.getContext('2d')
  if (!context) throw new Error('Card artwork is unavailable.')

  if (back) {
    context.fillStyle = '#081c19'
    context.fillRect(0, 85, 1024, 115)
    context.fillStyle = '#e8efdb'
    context.fillRect(72, 245, 880, 75)
    context.font = '26px monospace'
    context.fillStyle = '#173d30'
    context.fillText('SIMULATION ONLY', 94, 294)
    context.fillText('•••', 875, 294)
    context.fillStyle = '#f2f6e9'
    context.font = '600 32px "Segoe UI", sans-serif'
    context.fillText('Every purchase starts a signal.', 72, 395)
    context.font = '25px "Segoe UI", sans-serif'
    context.fillText('Fictional card · No real payments', 72, 443)
    context.font = '600 28px "Segoe UI", sans-serif'
    context.fillText('Credit Circuit', 742, 566)
    drawCircuit(context, 668, 523, 60, '#d6f58b')
  } else {
    context.fillStyle = '#f2f6e9'
    context.font = '600 40px "Segoe UI", sans-serif'
    context.fillText('Credit Circuit', 72, 104)
    context.font = '22px monospace'
    context.fillStyle = '#d6f58b'
    context.fillText('SIGNAL / CREDIT', 74, 145)

    const chip = context.createLinearGradient(840, 75, 930, 140)
    chip.addColorStop(0, '#f4e4ac')
    chip.addColorStop(.5, '#bc9754')
    chip.addColorStop(1, '#f8ecc5')
    context.fillStyle = chip
    context.beginPath()
    context.roundRect(846, 77, 90, 70, 12)
    context.fill()
    context.strokeStyle = '#765e34'
    context.lineWidth = 2
    context.stroke()
    for (const x of [875, 905]) {
      context.beginPath(); context.moveTo(x, 77); context.lineTo(x, 147); context.stroke()
    }
    for (const y of [100, 124]) {
      context.beginPath(); context.moveTo(846, y); context.lineTo(936, y); context.stroke()
    }
    drawCircuit(context, 676, 205, 245, '#b9dbba38')
    context.fillStyle = '#f2f6e9'
    context.font = '42px monospace'
    context.fillText(maskedNumber, 72, 390)
    context.font = '30px "Segoe UI", sans-serif'
    context.fillText(label, 72, 489, 650)
    context.font = '20px "Segoe UI", sans-serif'
    context.fillStyle = '#c9e2cd'
    context.fillText('VALID THRU', 806, 455)
    context.fillStyle = '#f2f6e9'
    context.font = '30px monospace'
    context.fillText(expiry, 806, 489)
    context.fillStyle = '#b6d8ad88'
    context.fillRect(72, 525, 880, 2)
    context.fillStyle = '#d6f58b'
    context.font = '20px monospace'
    context.fillText('FICTIONAL CARD', 72, 575)
    context.fillText('CC / 01', 852, 575)
  }
  return canvas
}

/** @param {CanvasRenderingContext2D} context @param {number} x @param {number} y @param {number} size @param {string} color */
function drawCircuit(context, x, y, size, color) {
  context.save()
  context.translate(x, y)
  context.scale(size / 40, size / 40)
  context.strokeStyle = color
  context.fillStyle = color
  context.lineWidth = 2.5
  context.lineCap = 'round'
  context.stroke(new Path2D('M29 8H15A7 7 0 0 0 8 15V25A7 7 0 0 0 15 32H29M29 16H18A2 2 0 0 0 16 18V22A2 2 0 0 0 18 24H29'))
  for (const yPosition of [8, 24]) {
    context.beginPath(); context.arc(29, yPosition, 2, 0, Math.PI * 2); context.fill()
  }
  context.restore()
}
