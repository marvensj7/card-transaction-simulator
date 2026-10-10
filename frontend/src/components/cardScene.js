import * as THREE from 'three'
import { drawCardArtwork } from './cardArtwork.js'

/** @param {HTMLElement} host @param {string} label @param {string} maskedNumber @param {string} expiry @param {() => void} onUnavailable */
export function createCardScene(host, label, maskedNumber, expiry, onUnavailable) {
  // A failure leaves the ordinary HTML/CSS card in place.
  const canvas = document.createElement('canvas')
  const context = canvas.getContext('webgl2', {alpha: true, antialias: true})
  if (!context) return null
  const renderer = new THREE.WebGLRenderer({canvas, context, alpha: true, antialias: true})
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  canvas.setAttribute('aria-hidden', 'true')
  host.appendChild(canvas)

  const scene = new THREE.Scene()
  const camera = new THREE.OrthographicCamera(-4.7, 4.7, 3, -3, .1, 100)
  camera.position.z = 15
  const model = new THREE.Group()
  scene.add(model)
  scene.add(new THREE.HemisphereLight(0xf3ffe0, 0x18372c, 3))
  const edgeLight = new THREE.DirectionalLight(0xe1ffbd, 4)
  edgeLight.position.set(-3, 5, 8)
  scene.add(edgeLight)

  // A rounded outline is extruded to give the card actual thickness.
  const shape = new THREE.Shape()
  const left = -4.28, right = 4.28, bottom = -2.7, top = 2.7, radius = .35
  shape.moveTo(left + radius, bottom)
  shape.lineTo(right - radius, bottom)
  shape.quadraticCurveTo(right, bottom, right, bottom + radius)
  shape.lineTo(right, top - radius)
  shape.quadraticCurveTo(right, top, right - radius, top)
  shape.lineTo(left + radius, top)
  shape.quadraticCurveTo(left, top, left, top - radius)
  shape.lineTo(left, bottom + radius)
  shape.quadraticCurveTo(left, bottom, left + radius, bottom)
  const bodyGeometry = new THREE.ExtrudeGeometry(shape, {depth: .12, bevelEnabled: true, bevelThickness: .025, bevelSize: .025, bevelSegments: 3, steps: 1})
  const edgeMaterial = new THREE.MeshStandardMaterial({color: 0xb3c986, metalness: .75, roughness: .3})
  const body = new THREE.Mesh(bodyGeometry, edgeMaterial)
  body.position.z = -.06
  model.add(body)

  const faceGeometry = new THREE.ShapeGeometry(shape)
  // Convert the shape's coordinates into texture coordinates from zero to one.
  const positions = faceGeometry.attributes.position
  const uv = faceGeometry.attributes.uv
  for (let index = 0; index < positions.count; index++) {
    uv.setXY(index, (positions.getX(index) + 4.28) / 8.56, (positions.getY(index) + 2.7) / 5.4)
  }
  const frontTexture = new THREE.CanvasTexture(drawCardArtwork(label, maskedNumber, expiry, false))
  const backTexture = new THREE.CanvasTexture(drawCardArtwork(label, maskedNumber, expiry, true))
  const frontMaterial = createFoilMaterial(frontTexture)
  const backMaterial = createFoilMaterial(backTexture)
  const front = new THREE.Mesh(faceGeometry, frontMaterial)
  front.position.z = .088
  const back = new THREE.Mesh(faceGeometry, backMaterial)
  back.position.z = -.088
  back.rotation.y = Math.PI
  model.add(front, back)

  let showBack = false
  let hovering = false
  let pointerX = 0, pointerY = 0
  let visible = true
  let dirty = true
  let previousTime = 0
  let hoverAmount = 0
  const motionPreference = window.matchMedia('(prefers-reduced-motion: reduce)')
  let reducedMotion = motionPreference.matches
  function updateMotionPreference() {
    reducedMotion = motionPreference.matches
    dirty = true
  }
  motionPreference.addEventListener('change', updateMotionPreference)
  function resize() {
    const {width, height} = host.getBoundingClientRect()
    if (!width || !height) return
    camera.top = 4.7 * height / width
    camera.bottom = -camera.top
    camera.updateProjectionMatrix()
    // CSS owns the displayed size; resizing only changes the drawing buffer.
    renderer.setSize(width, height, false)
    dirty = true
  }
  const resizeObserver = new ResizeObserver(resize)
  resizeObserver.observe(host)
  const visibilityObserver = new IntersectionObserver(entries => {
    visible = entries[0].isIntersecting
    dirty = true
  })
  visibilityObserver.observe(host)
  canvas.addEventListener('webglcontextlost', handleContextLoss)
  /** @param {Event} event */
  function handleContextLoss(event) {
    event.preventDefault()
    onUnavailable()
  }
  resize()

  // The face shader supplies a gradient, a hover highlight, and an idle sweep.
  // Text is composited last so shimmer cannot wash out the masked details.
  renderer.setAnimationLoop(milliseconds => {
    const delta = Math.min(milliseconds - previousTime, 50)
    previousTime = milliseconds
    if (!visible || document.hidden || (reducedMotion && !dirty)) return
    const time = milliseconds / 1000
    const easing = reducedMotion ? 1 : 1 - Math.exp(-delta / 100)
    hoverAmount += ((hovering ? 1 : 0) - hoverAmount) * easing
    const tiltX = reducedMotion ? 0 : hovering ? -pointerY * .14 : Math.sin(time * .6) * .025
    const tiltY = reducedMotion ? 0 : hovering ? pointerX * .18 : Math.sin(time * .4) * .04
    model.rotation.x += (tiltX - model.rotation.x) * easing
    model.rotation.y += ((showBack ? Math.PI : 0) + tiltY - model.rotation.y) * easing
    const scale = reducedMotion ? 1 : 1 + hoverAmount * .025
    model.scale.setScalar(scale)
    for (const material of [frontMaterial, backMaterial]) {
      material.uniforms.time.value = reducedMotion ? 0 : time
      material.uniforms.hover.value = hoverAmount
      material.uniforms.motion.value = reducedMotion ? 0 : 1
      material.uniforms.pointer.value.set(pointerX, pointerY)
    }
    renderer.render(scene, camera)
    dirty = false
  })

  return {
    /** @param {boolean} backVisible */
    setSide(backVisible) { showBack = backVisible; dirty = true },
    /** @param {boolean} active @param {number} x @param {number} y */
    setPointer(active, x, y) { hovering = active; pointerX = x; pointerY = y; dirty = true },
    dispose() {
      renderer.setAnimationLoop(null)
      resizeObserver.disconnect()
      visibilityObserver.disconnect()
      motionPreference.removeEventListener('change', updateMotionPreference)
      canvas.removeEventListener('webglcontextlost', handleContextLoss)
      bodyGeometry.dispose()
      faceGeometry.dispose()
      edgeMaterial.dispose()
      frontMaterial.dispose()
      backMaterial.dispose()
      frontTexture.dispose()
      backTexture.dispose()
      renderer.dispose()
      renderer.forceContextLoss()
      canvas.remove()
    },
  }
}

/** @param {THREE.CanvasTexture} artwork */
function createFoilMaterial(artwork) {
  return new THREE.ShaderMaterial({
    uniforms: {
      artwork: {value: artwork}, time: {value: 0}, hover: {value: 0},
      motion: {value: 1}, pointer: {value: new THREE.Vector2()},
    },
    vertexShader: `
      varying vec2 cardUv;
      void main() {
        cardUv = uv;
        gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
      }
    `,
    fragmentShader: `
      uniform sampler2D artwork;
      uniform float time, hover, motion;
      uniform vec2 pointer;
      varying vec2 cardUv;
      void main() {
        vec2 uv = cardUv;
        vec3 emerald = vec3(0.012, 0.038, 0.032);
        vec3 teal = vec3(0.014, 0.095, 0.11);
        vec3 gold = vec3(0.095, 0.115, 0.028);
        vec3 base = mix(emerald, teal, smoothstep(0.0, 1.3, uv.x + uv.y * 0.6));
        base = mix(base, gold, pow(1.0 - uv.y, 3.0) * 0.65);
        float rainbow = 0.5 + 0.5 * sin((uv.x + uv.y * 0.7 + pointer.x * 0.3) * 6.28);
        vec3 foil = mix(vec3(0.018, 0.16, 0.14), vec3(0.16, 0.145, 0.035), rainbow);
        base = mix(base, foil, hover * 0.75);
        float spotlight = max(0.0, 1.0 - distance(uv, pointer * 0.5 + 0.5));
        base += vec3(0.014, 0.018, 0.012) * spotlight * hover;
        float sweep = mod(time * 0.20, 2.3) - 0.6;
        float shine = pow(max(0.0, 1.0 - abs(uv.x + uv.y * 0.3 - sweep) / 0.20), 2.0);
        base += vec3(0.065, 0.075, 0.045) * shine * (1.0 - hover) * motion;
        vec4 ink = texture2D(artwork, uv);
        gl_FragColor = vec4(mix(base, ink.rgb, ink.a), 1.0);
        #include <colorspace_fragment>
      }
    `,
  })
}
