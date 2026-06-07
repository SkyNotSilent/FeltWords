#!/usr/bin/env ruby

require "base64"
require "fileutils"
require "json"
require "net/http"
require "openssl"
require "uri"

ROOT = File.expand_path("..", __dir__)
REFERENCE_PATH = File.join(ROOT, "FeltWords/Resources/Mascot/mascot-key-art.png")
OUTPUT_DIR = File.join(ROOT, "FeltWords/Resources/Mascot/DailyScenes")
SECRETS_PATH = File.join(ROOT, "Config/Secrets.xcconfig")
ENDPOINT = URI("https://apihub.agnes-ai.com/v1/images/generations")

SCENES = {
  "daily-eating" => "The three friends sit together at a small round breakfast table, happily eating a balanced meal with colorful vegetables, fruit, bread and milk.",
  "daily-learning" => "The three friends study together at a cozy low table, looking at a colorful English picture book and arranging simple alphabet cards.",
  "daily-playing" => "The three friends play together on a soft felt meadow, gently kicking a yellow ball and chasing soap bubbles.",
  "daily-tidying" => "The three friends happily put picture books, blocks and toys back into warm yellow and mint felt storage baskets.",
  "daily-bedtime" => "The three friends finish brushing their teeth and settle beside a soft cloud-shaped bed, ready to say good night."
}.freeze

def api_key
  line = File.readlines(SECRETS_PATH).find { |item| item.match?(/^\s*AGNES_API_KEY\s*=/) }
  key = line&.split("=", 2)&.last&.strip
  abort "Missing AGNES_API_KEY in #{SECRETS_PATH}" if key.nil? || key.empty? || key.include?("$(")
  key
end

def reference_data_url
  "data:image/png;base64,#{Base64.strict_encode64(File.binread(REFERENCE_PATH))}"
end

def request_image(key, prompt, reference)
  body = {
    model: "agnes-image-2.1-flash",
    prompt: prompt,
    size: "1024x1024",
    tags: ["img2img"],
    extra_body: {
      image: [reference],
      response_format: "url"
    }
  }

  request = Net::HTTP::Post.new(ENDPOINT)
  request["Authorization"] = "Bearer #{key}"
  request["Content-Type"] = "application/json"
  request.body = JSON.generate(body)

  response = Net::HTTP.start(
    ENDPOINT.host,
    ENDPOINT.port,
    use_ssl: true,
    open_timeout: 20,
    read_timeout: 180
  ) { |http| http.request(request) }

  abort "Agnes request failed (#{response.code}): #{response.body}" unless response.is_a?(Net::HTTPSuccess)
  url = JSON.parse(response.body).dig("data", 0, "url")
  abort "Agnes returned no image URL: #{response.body}" unless url
  URI(url)
end

def download_image(url, path)
  response = Net::HTTP.get_response(url)
  abort "Image download failed (#{response.code}): #{url}" unless response.is_a?(Net::HTTPSuccess)
  File.binwrite(path, response.body)
end

FileUtils.mkdir_p(OUTPUT_DIR)
reference = reference_data_url
key = api_key

SCENES.each_with_index do |(name, scene), index|
  output_path = File.join(OUTPUT_DIR, "#{name}.png")
  prompt = <<~PROMPT
    Create a polished square children's picture-book scene using the reference image as the strict character identity guide.
    Keep exactly the same three recurring felt-doll characters: the cheerful brown-haired girl in blue with a yellow scarf and backpack,
    the cheerful black-haired boy in mint green with a yellow scarf and backpack,
    and the same small golden-brown teddy bear named Mao Mao wearing round black glasses, a yellow cape and a stitched badge.
    Scene: #{scene}
    Preserve their recognizable faces, proportions, clothing colors, handmade wool felt texture and soft stitched edges.
    Warm cream backdrop with sunshine yellow, soft sky blue, mint and warm orange accents. Bright, gentle, playful and child-safe.
    Compose the characters mainly in the middle and lower area, leaving calm negative space near the top for an app text overlay.
    No written words, no letters, no logos, no watermark, no extra characters, no realistic human faces, no dark or scary elements.
  PROMPT

  puts "[#{index + 1}/#{SCENES.length}] Generating #{name}..."
  image_url = request_image(key, prompt, reference)
  download_image(image_url, output_path)
  puts "Saved #{output_path}"
end
